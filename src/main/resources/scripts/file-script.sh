#!/bin/sh
sudo touch "$2"
sudo chmod 777 "$2"
sudo ausearch -f "$1" -i | sudo tee "$2" > /dev/null