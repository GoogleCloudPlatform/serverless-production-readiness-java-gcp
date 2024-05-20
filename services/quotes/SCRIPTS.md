# Build & Test Quotes Service - Helpful Scripts

| Build Script              | Region | Scope |
| :---------------- | :------ | :---- |
| build.sh        |   Europe   | Build and push JIT image|
| build-us.sh        |   US   | Build and push JIT image|
| build-native.sh        |   Europe   | Build and push Native image|
| build-native-us.sh        |   US   | Build and push Native image|
| builds-cds.sh        |   Europe   | Build and push CDS image|
| builds-cds-us.sh        |   US   | Build and push CDS image|
| build-pgo.sh        |   Europe   | Build and push PGO image|
| build-pgo-us.sh        |   US   | Build and push PGO image|

| Deploy Script              | Region | Scope |
| :---------------- | :------: | ----: |
| deploy.sh        |   Europe   | Deploy JIT image|
| deploy-us.sh        |   US   | Deploy JIT image|
| deploy-native.sh        |   Europe   | Deploy Native image|
| deploy-native-us.sh        |   US   | Deploy Native image|
| deploys-cds.sh        |   Europe   | Deploy CDS image|
| deploys-cds-us.sh        |   US   | Deploy CDS image|
| deploy-pgo.sh        |   Europe   | Deploy PGO image|
| deploy-pgo-us.sh        |   US   | Deploy PGO image|

| Test Script              | Region | Scope |
| :---------------- | :------: | ----: |
| test.sh        |   Europe   | Test JIT image|
| test-us.sh        |   US   | Test JIT image|
| test-modern.sh        |   Europe   | Test Modern image|
| test-modern-us.sh        |   US   | Test Modern image|
| startlogs.sh        |   Europe   | Start logs|
| startlogs-us.sh        |   US   | Start logs|

| Local Testing              | Region | Scope |
| :---------------- | :------: | ----: |
| accurate.sh | Local | Start accurate-wait.sh script first then this script|
| accurate-wait.sh | Local | Start this script first|
| loadtests/localquotes.sh | Local | Start local test against Quote service|

