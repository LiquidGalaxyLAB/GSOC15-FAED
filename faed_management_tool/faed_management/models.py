from django.db import models


class City(models.Model):
    name = models.CharField(max_length=100)
    lat = models.FloatField(default='0')
    lng = models.FloatField(default='0')

    def __unicode__(self):
        return str(self.name)


class StyleURL(models.Model):
    name = models.CharField(max_length=50)
    href = models.URLField()
    scale = models.FloatField()

    def __unicode__(self):
        return str(self.name)


class DropPoint(models.Model):
    name = models.CharField(max_length=50)
    description = models.TextField()
    latitude = models.FloatField()
    longitude = models.FloatField()
    altitude = models.FloatField()
    is_available = models.BooleanField(default=True)
    style_url = models.ForeignKey(StyleURL)
    city = models.ForeignKey(City)

    def __unicode__(self):
        return str(self.name)


class Drone(models.Model):
    name = models.CharField(max_length=50)
    plate = models.CharField(max_length=50)
    origin_lat = models.FloatField(null=True)
    origin_lon = models.FloatField(null=True)
    destination_lat = models.FloatField(null=True)
    destination_lon = models.FloatField(null=True)
    # altitude = models.FloatField(default=50)
    emergency = models.CharField(max_length=50, null=True)
    battery_life = models.PositiveSmallIntegerField(default=100)
    style_url = models.ForeignKey(StyleURL)

    def __unicode__(self):
        return str(self.name + '-' + self.plate)


class Hangar(models.Model):
    name = models.CharField(max_length=50)
    description = models.TextField()
    latitude = models.FloatField()
    longitude = models.FloatField()
    altitude = models.FloatField()
    radius = models.FloatField()
    is_available = models.BooleanField(default=True)
    style_url = models.ForeignKey(StyleURL)
    drop_points = models.ManyToManyField(DropPoint)
    drone = models.ForeignKey(Drone)
    city = models.ForeignKey(City)

    def __unicode__(self):
        return str(self.name)


class MeteoStation(models.Model):
    name = models.CharField(max_length=50)
    description = models.TextField()
    latitude = models.FloatField()
    longitude = models.FloatField()
    altitude = models.FloatField()
    is_available = models.BooleanField(default=True)
    style_url = models.ForeignKey(StyleURL)

    tmp_now = models.FloatField(default=0.0)
    tmp_max = models.FloatField(default=0.0)
    tmp_min = models.FloatField(default=0.0)
    humidity = models.FloatField(default=0.0)
    precipitation = models.FloatField(default=0.0)
    pressure = models.FloatField(default=0.0)
    wind = models.FloatField(default=0.0)

    city = models.ForeignKey(City)

    def __unicode__(self):
        return str(self.name)


class Incidence(models.Model):
    lat = models.FloatField()
    long = models.FloatField()
    is_active = models.BooleanField(default=True)
    dropPoint = models.ForeignKey(DropPoint)
    hangar = models.ForeignKey(Hangar)
    # drone = models.ForeignKey(Drone)
