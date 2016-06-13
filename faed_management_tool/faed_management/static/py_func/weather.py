from faed_management_tool.settings import WEATHER_API_KEY
import json
import requests
import os
from kmls_management.models import Kml
from faed_management.static.py_func.sendtoLG import get_server_ip
from kmls_management.kml_generator import manage_kml, meteo_kml, weather_info

lleida = "3118514"


def get_weather(city_id, units):
    params = {'id': city_id, 'units': units, 'APPID': WEATHER_API_KEY}
    try:
        url = 'http://api.openweathermap.org/data/2.5/weather'
        response = requests.get(url=url, params=params)
        return response.text
    except KeyError:
        pass


def generate_weather(path):
    json_data = get_weather(lleida, "metric")
    data = json_loads_byteified(json_data)
    weather_info(
        path + "meteo_info.kml", data['main']['temp'],
        data['main']['temp_max'], data['main']['temp_min'],
        data['wind']['speed'], data['clouds']['all'],
        data['main']['pressure'], data['main']['humidity'],
        data['weather'][0]['description'])
    if not Kml.objects.filter(name="meteo_info.kml"):
        Kml(name="meteo_info.kml", url=path + "meteo_info.kml").save()


def can_fly():
    try:
        params = {'q': '3118514', 'units': 'metric', 'APPID': WEATHER_API_KEY}
        url = 'http://api.openweathermap.org/data/2.5/weather'
        response = requests.get(url=url, params=params)
        data = json.loads(response.text)
        if data['wind']['speed'] >= 10.0 or (data['rain']):
            print data['wind']['speed']
            print data['rain']
            return False
        else:
            return True
    except KeyError:
        pass


# Extracted from

def json_load_byteified(file_handle):
    return _byteify(
        json.load(file_handle, object_hook=_byteify),
        ignore_dicts=True
    )


def json_loads_byteified(json_text):
    return _byteify(
        json.loads(json_text, object_hook=_byteify),
        ignore_dicts=True
    )


def _byteify(data, ignore_dicts=False):
    # if this is a unicode string, return its string representation
    if isinstance(data, unicode):
        return data.encode('utf-8')
    # if this is a list of values, return list of byteified values
    if isinstance(data, list):
        return [_byteify(item, ignore_dicts=True) for item in data]
    # if this is a dictionary, return dictionary of byteified keys and values
    # but only if we haven't already byteified it
    if isinstance(data, dict) and not ignore_dicts:
        return {
            _byteify(key, ignore_dicts=True):
                _byteify(value, ignore_dicts=True)
            for key, value in data.iteritems()
            }
    # if it's anything else, return it in its original form
    return data
