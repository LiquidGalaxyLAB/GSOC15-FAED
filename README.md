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

## Installation

Get the latest git version:

```
git clone https://github.com/LiquidGalaxyLAB/FAED.git
```

Main directory: `faed_management_tool`

### Install dependencies

```
cd FAED/faed_management_tool
pip install -r requirements.txt 

```

## How to run

### Environment variables

Get maps api key from [Google developers](https://developers.google.com/)
Get weather api key from [Openweathermap](http://openweathermap.org/)

```
export MAPS_API_KEY=<API_KEY>
export WEATHER_API_KEY=<API_KEY>
```

### Run server

```
python manage.py runfaed <galaxy_ip> <server_ip>
```

### Exit server

Close terminal

License
-------
 
