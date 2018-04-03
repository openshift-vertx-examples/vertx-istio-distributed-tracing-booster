## Purpose 

Showcase Istio's Distributed Tracing capabilities with Eclipse Vert.x microservices.

## Install Istio

0. You need Minishift with OpenShift 3.7+. Enable the _admin_ addon:

```bash
minishift addon enable admin-user
```

Connect to the console using the `admin/admin` credentials and generate the _oc login_ token such as:

```bash
oc login https://192.168.64.34:8443 --token=YD7y0YZTwxC30RWI2l04CzEo8m-dW40ickw5Uvn4r78
``` 

1. Create Istio service accounts

```bash
oc adm policy add-scc-to-user anyuid -z istio-ingress-service-account -n istio-system
oc adm policy add-scc-to-user anyuid -z istio-grafana-service-account -n istio-system
oc adm policy add-scc-to-user anyuid -z istio-prometheus-service-account -n istio-system
```

2. Download Istio

```bash
curl -L https://git.io/getLatestIstio | sh -
```

3. Install Istio and Jaeger 

```bash
cd istio-??? # use the installed version
export PATH=$PWD/bin:$PATH
oc login -u system:admin
oc apply -f install/kubernetes/istio.yaml
oc apply -n istio-system -f https://raw.githubusercontent.com/jaegertracing/jaeger-kubernetes/master/all-in-one/jaeger-all-in-one-template.yml
oc expose service jaeger-query -n istio-system
```

4. Prepare the Namespace

_This mission assumes that myproject namespace is used._

Create the namespace if one does not exist

```bash
cd ..
oc new-project myproject
oc adm policy add-scc-to-user privileged -n myproject -z default
```

## Build and deploy the application

```bash
mvn package fabric8:build -Popenshift
oc apply -f suggestion-service/src/kubernetes/Service.yml 
oc create -f <(istioctl kube-inject -f suggestion-service/src/kubernetes/Deployment.yml)
oc apply -f store-service/src/kubernetes/Service.yml
oc create -f <(istioctl kube-inject -f store-service/src/kubernetes/Deployment.yml)
oc apply -f album-service/src/kubernetes/Service.yml
oc create -f <(istioctl kube-inject -f album-service/src/kubernetes/Deployment.yml)
oc apply -f album-details-service/src/kubernetes/Service.yml
oc create -f <(istioctl kube-inject -f album-details-service/src/kubernetes/Deployment.yml)
oc expose service suggestion-service
oc get route
```

You can use the application from the url indicated by the last command. Otherwise use the _suggestion-service_ route in 
the OpenShift dashboard. In the application, click a few times on the two _invoke_ buttons to generate traces.



## Seeing the traces

```bash
oc get route -n istio-system 
```

Use the indicated url. Then, click on the _service_ dropdown menu and select _suggestion-service_ and click on the _Find 
trace button_. You should see the traces. 
