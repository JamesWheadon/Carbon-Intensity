# CarbonIntensity

## Deploy Scheduler

```shell
cd scheduler
gcloud run deploy --source . scheduler --project=eighth-sandbox-442218-k5 --region=europe-west2 
```

for the moment, yes to unauthenticated invocations

### Remove scheduler

Delete artifact registry for the scheduler in [artifact registry](https://console.cloud.google.com/artifacts/docker/eighth-sandbox-442218-k5/europe-west2/cloud-run-source-deploy?project=eighth-sandbox-442218-k5)

```shell
gcloud run service delete scheduler --project=eighth-sandbox-442218-k5
```

## Deploying Web App

```shell
gcloud auth application-default login
```

```shell
./gradlew jib
```

More details on building JIB can be found at [https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin]()

Image is pushed to [artifact registry](https://console.cloud.google.com/artifacts/docker/eighth-sandbox-442218-k5/europe-west2/cloud-run-source-deploy/web-app?inv=1&invt=AbiEwQ&project=eighth-sandbox-442218-k5)

```shell
gcloud run deploy web-app \
    --image europe-west2-docker.pkg.dev/eighth-sandbox-442218-k5/cloud-run-source-deploy/web-app:latest \
    --platform managed \
    --region europe-west2 \
    --allow-unauthenticated
```
