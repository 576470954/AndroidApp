netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 listenport=8000 connectaddress=127.0.0.1 connectport=8000
netsh interface portproxy show all


adb forward tcp:8000 tcp:8000