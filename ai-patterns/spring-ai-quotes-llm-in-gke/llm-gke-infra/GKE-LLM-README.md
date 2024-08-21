# Inferencing OSS LLMs on GKE


## Prerequisite: Huggingface API token

Access to a Google Cloud project with the L4 GPUs available and enough quota in the region you select.
A computer terminal with kubectl and the Google Cloud SDK installed. From the GCP project console you’ll be working with, you may want to use the included Cloud Shell as it already has the required tools installed.
Some models such as Llama 3 will need Huggingface API token to download model files
Meta access request: https://ai.meta.com/resources/models-and-libraries/llama-downloads/ need regisgter an email address to download

Go to Hugging face, create account account with same email registered in Meta request. Then find Llama 3 model, fill out access request: https://huggingface.co/meta-llama/Meta-Llama-3.1-8B-Instruct .

## Setup project environments

From your console, select the Google Cloud region and project, checking that there’s availability for L4 GPUs in the one that you end up selecting. The one used in this tutorial is us-central, where at the time of writing this article there was availability for L4 GPUs( alternatively, you can choose other regions with different GPU accelerator type available):

```
export PROJECT_ID=<your-project-id>
export REGION=us-central1
export ZONE_1=${REGION}-a
export ZONE_2=${REGION}-b
export CLUSTER_NAME=vllm-serving-cluster
export NAMESPACE=vllm
export HF_TOKEN=<paste-your-own-token>
export OPENAPI_KEY=<paste-your-own-token>
gcloud config set project "$PROJECT_ID"
gcloud config set compute/region "$REGION"
gcloud config set compute/zone "$ZONE_1"
```
Then, enable the required APIs to create a GK cluster:
```
gcloud services enable compute.googleapis.com container.googleapis.com
```

Also, you may go ahead download the source code repo for this exercise, :
```
git clone https://github.com/llm-on-gke/vllm-inference.git
cd vllm-inference
```


In this exercise, you will be using the default service account to create the cluster, you need to grant it the required permissions to store metrics and logs in Cloud Monitoring that you will be using later on:

```
PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')
GCE_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
for role in monitoring.metricWriter stackdriver.resourceMetadata.writer; do
  gcloud projects add-iam-policy-binding $PROJECT_ID --member=serviceAccount:${GCE_SA} --role=roles/${role}
done
```
## Create GKE Cluster and Nodepools


### GKE Cluster

Now, create a GKE cluster with a minimal default node pool, as you will be adding a node pool with L4 GPUs later on:
```
gcloud container clusters create $CLUSTER_NAME \
  --workload-pool "${PROJECT_ID}.svc.id.goog" \
  --location "$REGION" \
  --enable-image-streaming --enable-shielded-nodes \
  --enable-ip-alias \
  --node-locations="$ZONE_1" \
  --shielded-secure-boot --shielded-integrity-monitoring \
  --workload-pool="${PROJECT_ID}.svc.id.goog" \
  --addons GcsFuseCsiDriver,HttpLoadBalancing \
  --num-nodes 1 --min-nodes 1 --max-nodes 5 \
  --ephemeral-storage-local-ssd=count=2 \
  --enable-ip-alias \
  --no-enable-master-authorized-networks \
  --machine-type n2d-standard-4
```

Here’s a breakdown of the parameters used in the `gcloud container clusters create` command for creating a GKE cluster:

1. **`--workload-pool "${PROJECT_ID}.svc.id.goog"`**:
   This enables Workload Identity for the cluster, which allows Kubernetes service accounts to act as IAM service accounts. The `${PROJECT_ID}.svc.id.goog` specifies the identity namespace associated with the GCP project.

2. **`--enable-image-streaming`**:
   Enables image streaming for the cluster, which speeds up the process of pulling container images by streaming only the parts of the image that are actually used, rather than downloading the entire image upfront.

3. **`--enable-shielded-nodes`**:
   This option enables Shielded Nodes, which provide enhanced node security through features like Secure Boot, integrity monitoring, and the use of a virtual Trusted Platform Module (vTPM). This helps protect against malicious software and other threats.

4. **`--shielded-secure-boot`**:
   A feature of Shielded Nodes, Secure Boot ensures that the system only boots up with signed software, protecting against malicious boot-level software.

5. **`--shielded-integrity-monitoring`**:
   Another Shielded Node feature, it provides a way to monitor and ensure the integrity of the operating system, protecting against unexpected changes.

6. **`--enable-ip-alias`**:
   Enables the use of IP aliases for the cluster. This feature allows for the separation of pod IP addresses from node IP addresses, facilitating network management and security. It also supports VPC-native clusters, which is beneficial for network performance and flexibility.

7. **`--node-locations="$ZONE_1"`**:
   Specifies the zones where the cluster's nodes will be located. By setting this, you can distribute nodes across multiple zones for high availability and fault tolerance.

8. **`--addons GcsFuseCsiDriver`**:
   Enables the GCS Fuse CSI Driver addon, allowing you to mount Google Cloud Storage buckets as filesystems in your Kubernetes pods. This is useful for accessing and storing large datasets or files directly in the cloud.

9. **`--no-enable-master-authorized-networks`**:
   This flag disables Master Authorized Networks, which restricts access to the Kubernetes API server to a set of authorized networks. Disabling this feature can simplify network configuration but may reduce security, so it should be done with caution.

10. **`--machine-type n2d-standard-4`**:
    Specifies the machine type for the nodes in the cluster. The `n2d-standard-4` type provides a balanced combination of compute, memory, and cost, and is based on AMD EPYC processors.

11. **`--num-nodes 1 --min-nodes 1 --max-nodes 5`**:
    These parameters set the initial, minimum, and maximum number of nodes in the cluster. This allows for automatic scaling of the node pool based on workload demands, starting with 1 node and scaling up to 5 nodes as needed.

12. **`--ephemeral-storage-local-ssd=count=2`**:
    This parameter attaches two local SSDs to each node for ephemeral storage, providing fast local storage that can be used for tasks like caching, temporary files, and more.

13. **`--enable-ip-alias` (repeated)**:
    This option is repeated and ensures that the IP aliasing feature is enabled for the cluster, as explained above.

These settings together provide a robust, secure, and scalable environment for deploying applications and workloads on GKE, especially those that require specific hardware like GPUs or need enhanced security features.


### Nodepool

### Quick Estimates of GPU for serving:
Reference: https://www.baseten.co/blog/llm-transformer-inference-guide/

Create an additional Spot node pool with regular (we use spot to illustrate) VMs with 2 L4 GPUs each:
```
gcloud container node-pools create g2-standard-24 --cluster $CLUSTER_NAME \
  --num-nodes=1 --min-nodes=0 --max-nodes=2 \
  --shielded-secure-boot \
  --accelerator type=nvidia-l4,count=1,gpu-driver-version=latest \
  --machine-type g2-standard-8 \
  --ephemeral-storage-local-ssd=count=1 \
  --enable-autoscaling --enable-image-streaming \
  --shielded-integrity-monitoring \
  --node-locations $ZONE_1,$ZONE_2 --region $REGION --spot
```
Here's a breakdown of the parameters used in the `gcloud container node-pools create` command:

1. **`gcloud container node-pools create g2-standard-24`**:
    - This command creates a new node pool named `g2-standard-24` within an existing GKE cluster.

2. **`--cluster $CLUSTER_NAME`**:
    - Specifies the name of the GKE cluster to which the node pool belongs. The `$CLUSTER_NAME` variable should contain the name of your cluster.

3. **`--accelerator type=nvidia-l4,count=1,gpu-driver-version=latest`**:
    - **`type=nvidia-l4`:** Specifies the type of GPU to be used in the node pool, in this case, NVIDIA L4 GPUs.
    - **`count=1`:** Indicates that each node in this pool will have 1 GPU.
    - **`gpu-driver-version=latest`:** Installs the latest NVIDIA GPU driver on the nodes.

4. **`--machine-type g2-standard-8`**:
    - Specifies the machine type for the nodes in the node pool. `g2-standard-8` is a machine type that includes 8 vCPUs and a certain amount of memory, optimized for use with GPUs.

5. **`--ephemeral-storage-local-ssd=count=1`**:
    - Adds one local SSD to each node for ephemeral storage. This is useful for high-performance, temporary storage needs, such as caching or temporary data during processing.

6. **`--enable-autoscaling`**:
    - Enables autoscaling for the node pool. This allows the node pool to automatically adjust the number of nodes based on the cluster's workload and resource needs.

7. **`--enable-image-streaming`**:
    - Enables image streaming, which allows the nodes to download only the necessary parts of container images needed to start the application, reducing startup time and improving efficiency.

8. **`--num-nodes=1 --min-nodes=0 --max-nodes=2`**:
    - **`--num-nodes=1`:** Sets the initial number of nodes in the node pool to 1.
    - **`--min-nodes=0`:** Sets the minimum number of nodes in the pool to 0, allowing the pool to scale down to zero nodes if not needed.
    - **`--max-nodes=2`:** Sets the maximum number of nodes in the pool to 2, allowing the pool to scale up as needed.

9. **`--shielded-secure-boot`**:
    - Enables Secure Boot, a security feature that ensures the node boots only with verified and signed software, protecting against unauthorized software.

10. **`--shielded-integrity-monitoring`**:
- Enables integrity monitoring, which checks for changes to the system and ensures the integrity of the node's operating system and configuration.

11. **`--node-locations $ZONE_1,$ZONE_2`**:
- Specifies the zones where the nodes will be located. This parameter helps distribute the nodes across multiple availability zones, enhancing high availability and fault tolerance.

12. **`--region $REGION`**:
- Specifies the region in which the node pool is created. The `$REGION` variable should contain the specific region, such as `us-central1`.

13. **`--spot`**:
- Indicates that the node pool uses Spot VMs, which are spare compute instances available at a lower cost but can be preempted by Google Cloud if needed. This is a cost-effective option for workloads that can tolerate interruptions.

*These parameters collectively define the configuration and behavior of the node pool, optimizing it for specific use cases like running GPU-accelerated workloads while also managing costs and ensuring security.


After a few minutes, check that the node pool was created correctly:
```
kubectl get nodes;
kubectl describe nodes;
```

Run the following commands to setup identity, Huggingface token, Open AI token (you can set this to whatever you want) and IAM roles:
```
kubectl create ns $NAMESPACE

gcloud container clusters get-credentials $CLUSTER_NAME $REGION
export HF_TOKEN=<paste-your-own-token>
kubectl create secret generic huggingface --from-literal="HF_TOKEN=$HF_TOKEN" -n $NAMESPACE
export OPENAPI_KEY=<paste-your-own-token>
kubectl create secret generic openapikey --from-literal="key=$OPENAPI_KEY" -n $NAMESPACE

kubectl create serviceaccount $NAMESPACE --namespace $NAMESPACE
gcloud iam service-accounts add-iam-policy-binding $GCE_SA \
    --role roles/iam.workloadIdentityUser \
    --member "serviceAccount:${PROJECT_ID}.svc.id.goog[${NAMESPACE}/${NAMESPACE}]"

kubectl annotate serviceaccount $NAMESPACE \
    --namespace $NAMESPACE \
    iam.gke.io/gcp-service-account=$GCE_SA
```

## Deploy model to GKE cluster
We’re now ready to deploy the model.
Save the following vllm-deploy.yaml,
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: vllm-server
  labels:
    app: vllm-server
spec:
  replicas: 1
  selector:
    matchLabels:
      app: vllm-inference-server
  template:
    metadata:
      labels:
        app: vllm-inference-server
    spec:
      volumes:
       - name: cache
         emptyDir: {}
       - name: dshm
         emptyDir:
              medium: Memory
      nodeSelector:
        cloud.google.com/gke-accelerator: nvidia-l4
      serviceAccountName: vllm
      containers:
        - name: vllm-inference-server
          image: vllm/vllm-openai
          imagePullPolicy: IfNotPresent
          resources:
            requests:
              cpu: 7
              memory: 24Gi
              nvidia.com/gpu : 1
              ephemeral-storage: 80Gi
            limits:
              cpu: 7
              memory: 24Gi
              nvidia.com/gpu : 1
              ephemeral-storage: 80Gi
          env:
            - name: HUGGING_FACE_HUB_TOKEN
              valueFrom:
                secretKeyRef:
                  name: huggingface
                  key: HF_TOKEN
            - name: TRANSFORMERS_CACHE
              value: /.cache
            - name: shm-size
              value: 1g
            - name: VLLM_API_KEY
              valueFrom:
                secretKeyRef:
                  name: openapikey
                  key: key
          command: ["python3", "-m", "vllm.entrypoints.openai.api_server"]
          args: ["--model=meta-llama/Meta-Llama-3.1-8B-Instruct",
                 "--gpu-memory-utilization=0.95",
                 "--disable-log-requests",
                 "--trust-remote-code",
                 "--port=8000",
                 "--max-model-len=4000",
                 "--tensor-parallel-size=1"]
          ports:
            - containerPort: 8000
              name: http
          securityContext:
            runAsUser: 1000
          volumeMounts:
            - mountPath: /dev/shm
              name: dshm
            - mountPath: /.cache
              name: cache

---
apiVersion: v1
kind: Service
metadata:
  name: vllm-inference-server
  labels:
    app: vllm-inference-server
  annotations:
    cloud.google.com/neg: '{"ingress": true}'
    cloud.google.com/backend-config: '{"default": "vllm-backendconfig"}'
spec: 
  type: ClusterIP
  ports:
    - name: http-inference-server
      port: 8000
      targetPort: 8000 #nodePort: 30036
      protocol: TCP
  selector:
    app: vllm-inference-server
---
apiVersion: cloud.google.com/v1
kind: BackendConfig
metadata:
  name: vllm-backendconfig
spec:
  # gRPC healthchecks not supported, use http endpoint instead https://cloud.google.com/kubernetes-engine/docs/how-to/ingress-configuration#direct_health
  healthCheck:
    checkIntervalSec: 15
    timeoutSec: 15
    healthyThreshold: 1
    unhealthyThreshold: 2
    type: HTTP                      # GKE Ingress controller only supports HTTP, HTTPS, or HTTP2
    requestPath: /health   # Not a real endpoint, but should work (via prometheus metrics exporter)
    port: 8000
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: vllm-ingress
spec:
  defaultBackend:
    service:
      name: vllm-inference-server
      port:
        number: 8000
```            


Notes:
official container image to run the modles: vllm/vllm-openai


## vLLM model config parameters:

You can override the command and arguments,

native vLLM APIs:

command: ["python3", "-m", "vllm.entrypoints.api_server"]

openai api server:

command: ["python3", "-m", "vllm.entrypoints.openai.api_server"]

## Deploy the model to GKE
After vllm-deploy.yaml file been updated with proper settings, execute the followin command:
```
kubectl apply -f vllm-deploy-llama3-1-hf.yaml -n $NAMESPACE 
```
The following GKE artifacts will be created:
a. vllm-server deployment
b. Ingress
b. Service with endpoint of LLM APIs, routing traffic through Ingress

Check all the objects you’ve just created:

kubectl get all
Check that the pod has been correctly scheduled in one of the nodes in the g2-standard-8 node pool that has the GPUs available:


## Tests

Simplely run the following command to get the cluster ip:
```
kubectl get ingress -n "$NAMESPACE"
export INGRESS_CLUSTER_IP=$(kubectl get ingress -n "$NAMESPACE" | awk 'NR==2 {print $4}')
echo $INGRESS_CLUSTER_IP
```

Then use the following curl command to test inside the Cluster(update the cluster IP first):
```
curl -X POST \
    "http://$INGRESS_CLUSTER_IP/v1/chat/completions" \
    -H "Connection: keep-alive" \
    -H "Accept: application/json" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $OPENAPI_KEY" \
    -d '{"messages": [{"role": "system", "content": "Answer like an experienced literary professor."}, {"role": "user", "content": "Answer like an experienced literary professor; please provide a quote from a random book, including book, quote and author; do not repeat quotes from the same book; return the answer wrapped in triple backquotesjsonstrictly in JSON format"}], "model": "meta-llama/Meta-Llama-3.1-8B-Instruct"}' \
    --compressed
```

```
output:

{
  "id": "chat-4adf824b2645adfa9f78a996ab2b2eb8",
  "object": "chat.completion",
  "created": 1724268250,
  "model": "meta-llama/Meta-Llama-3.1-8B-Instruct",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "```json\n{\n  \"quote\": \"It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.\",\n  \"book\": \"Pride and Prejudice\",\n  \"author\": \"Jane Austen\"\n}\n```",
        "tool_calls": []
      },
      "logprobs": null,
      "finish_reason": "stop",
      "stop_reason": null
    }
  ],
  "usage": {
    "prompt_tokens": 88,
    "total_tokens": 147,
    "completion_tokens": 59
  }
}
```

Spin up or down node pool
```
gcloud container clusters resize $CLUSTER_NAME --node-pool g2-standard-24 --num-nodes 1 --region us-central1
kubectl apply -f vllm-deploy-llama3-1-hf.yaml -n vllm


#scale down
gcloud container clusters resize $CLUSTER_NAME --node-pool g2-standard-24 --num-nodes 0 --region us-central1
kubectl delete deployment vllm-server -n vllm
```

