Welcome to FAED project
=======================

The main idea of this project is to use a drone to bring flying an automated electronic defibrillator (AED) where a cardiac emergency is happening. This drone will have a support platform with an easy extraction system in order to get the AED as fast as possible. But sideways the project is the base block for any kind of drone based distribution project.

The web site was created to highlight our ideas and promotion: http://www.faeddroneproject.com/


Contributing
------------
This repository wants to be the mix of the 2 projects that have been working in order to deploy this idea. Here you can find the contributors and the original repository

- https://github.com/aksonuabhay/IS-Erle
- https://github.com/FaedDroneLogistics/gsoc15

Prerequisites
-------------

- [InteractiveSpaces](http://www.interactive-spaces.org)
- [LiquidGalaxy](https://code.google.com/p/liquid-galaxy/)
- [DJango](https://www.djangoproject.com)
- [ROS](http://www.ros.org/)
- [MAVLink](http://qgroundcontrol.org/mavlink/start)


###1. Install packages:
```
apt-get install git python-pip ssh sshpass
pip install virtualenvwrapper
```

###2. Get the latest git version and go inside:
```
git clone https://github.com/LiquidGalaxyLAB/FAED.git
```

###3. Create environment and install dependencies:
```
mkvirtualenv faed
cd FAED/faed_management_tool
pip install -r requeriments.txt
```

###4. Export environment variables

Get maps api key from [Google developers](https://developers.google.com/)
Get weather api key from [Openweathermap](http://openweathermap.org/)

```
echo 'export MAPS_API_KEY=<API_KEY>
echo 'export WEATHER_API_KEY=<API_KEY>
```

###5. Run server
```
python manage.py runfaed <galaxy_ip> <server_ip>
```

Or

```
faed-start <galaxy_ip> <server_ip>
```
