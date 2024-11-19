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

## Package
```
./gradlew jib
```

More details on building JIB can be found at [https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin]()

