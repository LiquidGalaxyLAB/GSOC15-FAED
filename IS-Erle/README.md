# IS-Erle
This project is a part of [FAED Drone Project](http://www.faeddroneproject.com/) and is a [
GSoC 2015](https://www.google-melange.com/gsoc/homepage/google/gsoc2015) project. 
This subproject has to maintain a robust communication between a drone and Interactive Spaces activities. 
It has to make the drone follow a particular path by sending it coordinates to the Way points. 
It also has to receive data back from the drone so that it can be monitored in real time. 

## Prerequisites

- [InteractiveSpaces](http://www.interactive-spaces.org)
- [LiquidGalaxy](https://code.google.com/p/liquid-galaxy/)
- [ROS](http://www.ros.org/)
- [MAVLink](http://qgroundcontrol.org/mavlink/start)

## Getting Started
The easiest way to get started is to clone the repository:

```bash

git clone https://github.com/aksonuabhay/IS-Erle.git

```

## Building
Once you have all the prerequisites installed, you can build this project.
Clone it in the interactivespaces workbench folder. 
Copy the jar file in the dependencies folder to  bootsrap folder of interactivespaces controller.
And then run :

```bash

./bin/isworkbench walk build

```

This will build all the activities provided all the depencencies are satisfied.

## Installing
After running interactivespaces master and controller open a web browser and
go [here](localhost:8080/interactivespaces/). There follow the instructions of the interactivespaces docs.
The web interface is simple and easy for a person who knows the basics of interactivespaces.

## Running
To launch all the activities and start communicating with a drone, first connect the flight controller
to your system and then run the following in the IS-Erle folder:

```bash

python start.py

```

To view data published on some important topics:

```bash

python data_display.py

```

## Contributing
You can always contribute to this repositpory. Just fork it and do a pull request for adding updates.
Code documentation can be found [here](http://aksonuabhay.github.io/IS-Erle/Documentation/index.html).
This repository is dependent on another project for mission data. You can contribute to the
[FaedDroneLogistics](https://github.com/FaedDroneLogistics/gsoc15) repository too!

## License
You can use this project as you wish. However, the contributors are not going to be responsible for any damage.
Feel free to report any bugs, generate pull requests.
