cd github/appengine-plugins-core

wget https://dl.google.com/dl/cloudsdk/channels/rapid/GoogleCloudSDKInstaller.exe
GoogleCloudSDKInstaller.exe /S /noreporting /nostartmenu /nodesktop /logtofile /D=T:\google
t:\google\gcloud.cmd components copy-bundled-python>>python_path.txt && SET /p CLOUDSDK_PYTHON=<python_path.txt && DEL python_path.txt
t:\google\gcloud.cmd components update --quiet
t:\google\gcloud.cmd components install app-engine-java --quiet

mvn clean install cobertura:cobertura -B -U
curl -s https://codecov.io/bash | bash

exit /b %ERRORLEVEL%