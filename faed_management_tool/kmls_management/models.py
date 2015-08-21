from django.db import models

# Create your models here.
class Kml(models.Model):
    name = models.CharField(max_length=50)
    url = models.CharField(max_length=100)
    visibility = models.BooleanField(default=True)

    def __unicode__(self):
        return str(self.name)


