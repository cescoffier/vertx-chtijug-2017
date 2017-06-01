# Vert.x + OpenShift - Live coding session

## Requirements

1. Install Minishift from https://github.com/minishift/minishift
2. Start minishift with `minishift start`
3. Create the project using `script/prepare-project.sh`

## Deployment of the shopping-backend

```bash
cd shopping-backend
../scripts/build-with-docker.sh
```

## Populate database

```bash
../scripts/populate.sh
```

## Deployment of the shopping-list-service

```bash
cd shopping-list-service
mvn fabric8:deploy
```

## Toggle pricer service

```bash
scripts/toggle-pricer.sh
```
