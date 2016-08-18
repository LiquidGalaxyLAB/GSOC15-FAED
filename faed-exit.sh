#!/usr/bin/env bash

sshpass -p 'lqgalaxy' ssh killall python
sshpass -p 'lqgalaxy' ssh lg@$galaxy_ip \"echo '' > /var/www/html/kmls.txt\"
sshpass -p 'lqgalaxy' ssh lg@$galaxy_ip \"echo '' > /var/www/html/kmls_4.txt\"