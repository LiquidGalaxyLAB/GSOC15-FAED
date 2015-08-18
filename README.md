#FAED

Welcome to FAED project
=======================



*Note: We're currently very close to moving from Express to Loopback. As such, please keep in mind that the instructions here for setting up and running the project do not directly translate to the staging branch. Additionally, the file structure is quite a bit different. As always, the staging branch is the appropriate place to branch off of to fix/add something.*

Wiki
------------

We would love your help expanding our [wiki](https://github.com/freecodecamp/freecodecamp/wiki) with more information about learning to code and getting a coding job.

Contributing
------------



Prerequisites
-------------

- [MongoDB](http://www.mongodb.org/downloads)
- [Node.js](http://nodejs.org)

Getting Started
---------------

The easiest way to get started is to clone the repository:

```bash
# Get the latest snapshot
git clone --depth=1 https://github.com/freecodecamp/freecodecamp.git freecodecamp

cd freecodecamp

# Install NPM dependencies
npm install

# Install Bower dependencies
bower install

# Create a .env file and populate it with the necessary API keys and secrets:
touch .env

```

Edit your .env file with the following API keys accordingly (if you only use email login, only the MONGOHQ_URL, SESSION_SECRET, MANDRILL_USER and MANDRILL_PASSWORD fields are necessary. Keep in mind if you want to use more services you'll have to get your own API keys for those services.

`
```

License
-------
 
