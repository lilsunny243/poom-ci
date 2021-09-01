# Prepare deployment zip
```
cd ~/workspaces/deployments
zip -r deployment-base.zip deployment-base -x "deployment-base/.*"
mv deployment-base.zip ~/workspaces/poom-ci/poom-ci/poom-ci-utilities/src/test/resources/
```