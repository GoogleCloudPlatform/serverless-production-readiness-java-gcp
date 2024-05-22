# Build & Test Quotes Service - Helpful Scripts

| Build Script              | Region | Scope |
| :---------------- | :------ | :---- |
| JIT ||
| build.sh        |   Europe   | Build and push JIT image|
| build-us.sh        |   US   | Build and push JIT image|
| Native ||
| build-native.sh        |   Europe   | Build and push Native image|
| build-native-us.sh        |   US   | Build and push Native image|
| CDS ||
| builds-cds.sh        |   Europe   | Build and push CDS image|
| builds-cds-us.sh        |   US   | Build and push CDS image|
| PGO ||
| build-pgo.sh        |   Europe   | Build and push PGO image|
| build-pgo-us.sh        |   US   | Build and push PGO image|
| Projecy Leyden
| build-leyden-premain.sh | Europe | Build and push Leyden image|
| build-leyden-premain-us.sh | US | Build and push Leyden image|


| Deploy Script              | Region | Scope |
| :---------------- | :------ | :---- |
| JIT ||
| deploy.sh        |   Europe   | Deploy JIT image|
| deploy-us.sh        |   US   | Deploy JIT image|
| Native ||
| deploy-native.sh        |   Europe   | Deploy Native image|
| deploy-native-us.sh        |   US   | Deploy Native image|
| CDS ||
| deploys-cds.sh        |   Europe   | Deploy CDS image|
| deploys-cds-us.sh        |   US   | Deploy CDS image|
| PGO ||
| deploy-pgo.sh        |   Europe   | Deploy PGO image|
| deploy-pgo-us.sh        |   US   | Deploy PGO image|
| Project Leyden
| deploy-leyden-premain.sh | Europe | Deploy Project Leyden image|
| deploy-leyden-premain-us.sh | US | Deploy Project Leyden image|


| Test Script              | Region | Scope |
| :---------------- | :------ | :---- |
| test.sh        |   Europe   | Test JIT image|
| test-us.sh        |   US   | Test JIT image|
| test-modern.sh        |   Europe   | Test Modern image|
| test-modern-us.sh        |   US   | Test Modern image|
| startlogs.sh        |   Europe   | Start logs|
| startlogs-modern.sh        |   Europe   | Start logs - CDS, Native, PGO, Project Leyden|
| startlogs-us.sh        |   US   | Start logs|
| startlogs-us-modern.sh        |   Europe   | Start logs - CDS, Native, PGO, Project Leyden|

| Local Testing              | Region | Scope |
| :---------------- | :------ | :---- |
| accurate.sh | Local | Start accurate-wait.sh script first then this script|
| accurate-wait.sh | Local | Start this script first|
| loadtests/localquotes.sh | Local | Start local test against Quote service|

