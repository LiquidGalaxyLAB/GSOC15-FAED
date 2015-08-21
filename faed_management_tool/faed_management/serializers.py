import models
from rest_framework import serializers

class DropPointSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = models.DropPoint
        fields = ('id', 'name', 'description', 'latitude', 'longitude', 'altitude',
                  'is_available')

class HangarSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = models.Hangar
        fields = ('id', 'latitude', 'longitude', 'altitude', 'radius', 'is_available')

class MeteoStationSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = models.MeteoStation
        fields = ('id', 'name', 'description', 'latitude', 'longitude', 'altitude',
                  'temperature', 'wind_speed')