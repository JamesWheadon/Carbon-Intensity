# CarbonIntensity

## Deploy Scheduler

```shell
cd scheduler
gcloud run deploy --source . scheduler --project=eighth-sandbox-442218-k5 --region=europe-west2 
```

for the moment, yes to unauthenticated invocations

## Package
```
./gradlew jib
```

More details on building JIB can be found at [https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin]()

