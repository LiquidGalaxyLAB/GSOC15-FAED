from django import forms

import models

class StyleURLForm(forms.ModelForm):
    class Meta:
        model = models.StyleURL
        exclude = []


class CityForm(forms.ModelForm):

    class Meta:
        model = models.City
        exclude = ['lat', 'lng']

class DropPointForm(forms.ModelForm):
    description = forms.CharField(widget=forms.Textarea(attrs={'class':'materialize-textarea'}));

    class Meta:
        model = models.DropPoint
        exclude = []

class DroneForm(forms.ModelForm):
    class Meta:
        model = models.Drone
        exclude = ['origin_lat', 'origin_lon', 'destination_lat', 'destination_lon', 'emergency', 'battery_life']

class HangarForm(forms.ModelForm):
    description = forms.CharField(widget=forms.Textarea(attrs={'class':'materialize-textarea'}));

    class Meta:
        model = models.Hangar
        exclude = ['drop_points']

class MeteoStationForm(forms.ModelForm):
    description = forms.CharField(widget=forms.Textarea(attrs={'class':'materialize-textarea'}));

    class Meta:
        model = models.MeteoStation
        exclude = ['tmp_now','tmp_max','tmp_min','humidity','precipitation','pressure','wind']


#class MeteoStationForm(forms.Form):
#    name = forms.CharField(label='name')
#    description = forms.CharField(widget=forms.Textarea,label='description')
#    latitude = forms.FloatField(label='latitude')
#    longitude = forms.FloatField(label='longitude')
#    altitude = forms.FloatField(label='altutude')
#    is_available = forms.BooleanField(label='is_available')
#    temperature = forms.FloatField(label='temperature')
#    wind_speed = forms.FloatField(label='wind_speed')
#    style_url = forms.ChoiceField(label='style_url')

#    def __init__(self, *args, **kwargs):
#        super(MeteoStationForm, self).__init__(*args, **kwargs)
#        self.fields['style_url'].choices = [(style.id, style.name) for style in models.StyleURL.objects.all()]
