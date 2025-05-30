# CarbonIntensity

App is available at https://react-app-7vauz33abq-nw.a.run.app/

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
    --allow-unauthenticated \
    --update-env-vars SCHEDULER_URL=https://scheduler-1088477649607.europe-west2.run.app
```

## Deployed React App

Deployment is done using cloud build, a manual trigger exists to run the build.

If there is a forbidden issue when accessing the app, check IAM policy for the service.
```shell
gcloud run services get-iam-policy react-app --region=europe-west2
```
If the roles/run.invoker is not present for allUsers, add the permission to all users.
```shell
gcloud run services add-iam-policy-binding react-app-service \
    --member="allUsers" \
    --role="roles/run.invoker" \
    --region=us-central1
```

## Running Jaeger locally

### Starting container

```shell
docker run --rm -it --name jaeger \
  -e COLLECTOR_OTLP_ENABLED=true \
  -p 4317:4317 \
  -p 16686:16686 \
  jaegertracing/all-in-one:1.68.0
```

### Stopping container

```shell
docker stop jaeger
```
