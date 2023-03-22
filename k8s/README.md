1. create configmap
``` sh
kubectl create configmap patience-configmap --from-file=patience.edn
```
1.5 optional. print configmap
``` sh
kubectl get configmaps patience-configmap -o yaml
```
2. Create all infra (exclude config map) via one command:
```
kubectl create -f patience.yml
```
3. To get access to ingress port with app:
```
kubectl port-forward --namespace=ingress-nginx service/ingress-nginx-controller 8080:80
```
4. Enjoy the app on http://patience.localdev.me:8080/patients (if you have a local k8s cluster or correct dns config)

5. optional. Expose pods nREPL port
```
kubectl port-forward patience-deployment-84449bcfb5-j6h4c 7888:7888
```
