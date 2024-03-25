package services;

import static org.junit.Assert.assertNotNull;

import com.google.api.core.ApiFuture;
import com.google.cloud.NoCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;

import services.domain.FirestoreService;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.FirestoreEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
// @ActiveProfiles("test")
public class FirestoreServiceTests {
  @BeforeEach
  public void setup() {
    FirestoreOptions options = FirestoreOptions.getDefaultInstance().toBuilder()
        .setHost(firestoreEmulator.getEmulatorEndpoint())
        .setCredentials(NoCredentials.getInstance())
        .setProjectId("fake-test-project-id")
        .build();
    Firestore firestore = options.getService();

    this.eventService = new FirestoreService(options, firestore);
  }

  @Container
  private static final FirestoreEmulatorContainer firestoreEmulator =
      new FirestoreEmulatorContainer(
          DockerImageName.parse(
              "gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators"));

  @DynamicPropertySource
  static void emulatorProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.cloud.gcp.firestore.host-port", firestoreEmulator::getEmulatorEndpoint);
  }

  // @Autowired
  private FirestoreService eventService;

  @Test
  void testEventRepositoryStoreBook() throws ExecutionException, InterruptedException {
    ApiFuture<WriteResult> writeResult = eventService.storeBookInfo("The_Jungle_Book-Rudyard_Kipling-1894-public.txt",
      "The Jungle Book",
      "Rudyard Kipling",
      "The Jungle Book is a collection of stories by the English author Rudyard Kipling. Most of the characters are animals such as Shere Khan the tiger and Baloo the bear, though a principal character",
      "modelResponse");
    assertNotNull(writeResult.get().getUpdateTime());
  }
}