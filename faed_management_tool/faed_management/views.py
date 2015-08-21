import os
import sys
import json
import requests

from django.contrib.gis.measure import D
from django.contrib.gis.geos.point import Point
from django.views.generic import ListView
from django.shortcuts import render
from django.http import HttpResponseRedirect, HttpResponse
from rest_framework import viewsets

import forms
import models
from kmls_management.models import Kml
from faed_management.static.py_func.sendtoLG import sync_kmls_to_galaxy, sync_kmls_file
from kmls_management import kml_generator
from serializers import HangarSerializer, DropPointSerializer, MeteoStationSerializer
from faed_management.models import Hangar, DropPoint, MeteoStation, StyleURL, Drone
from faed_management.forms import HangarForm, MeteoStationForm, DropPointForm, StyleURLForm, DroneForm


# List items
class HangarsList(ListView):
    model = Hangar

class HangarsView(ListView):
    template_name = 'hangars.html'
    context_object_name = 'hangars'
    queryset = models.Hangar.objects.all()
    success_url = "/hangars"

def hangar_per_city(request):
    hangars = models.Hangar.objects.all()
    cities = models.City.objects.all()

    sorted_dict = {}

    for hangar in hangars:
        try:
            sorted_dict[hangar.city].append(hangar)
        except KeyError:
            sorted_dict[hangar.city] = [hangar]

    return render(request, 'hangars.html', {'cities':cities, 'hangars_city': sorted_dict})

class MeteoStationsList(ListView):
    model = MeteoStation


class MeteoStationsView(ListView):
    template_name = 'meteostations.html'
    context_object_name = 'meteostations'
    queryset = models.MeteoStation.objects.all()
    success_url = "/meteostations"

def meteostations_per_city(request):
    meteostations = models.MeteoStation.objects.all()
    cities = models.City.objects.all()

    sorted_dict = {}

    for meteostation in meteostations:
        try:
            sorted_dict[meteostation.city].append(meteostation)
        except KeyError:
            sorted_dict[meteostation.city] = [meteostation]

    return render(request, 'meteostations.html', {'cities':cities, 'meteostations_city': sorted_dict})

class DropPointsList(ListView):
    model = DropPoint


class DropPointsView(ListView):
    template_name = 'droppoints.html'
    context_object_name = 'droppoints'
    queryset = models.DropPoint.objects.all()
    success_url = "/droppoints"

def droppoint_per_city(request):
    droppoints = models.DropPoint.objects.all()
    cities = models.City.objects.all()

    sorted_dict = {}

    for droppoint in droppoints:
        try:
            sorted_dict[droppoint.city].append(droppoint)
        except KeyError:
            sorted_dict[droppoint.city] = [droppoint]

    return render(request, 'droppoints.html', {'cities':cities, 'droppoints_city': sorted_dict})
# Forms

def submit_city(request):
    if request.method == 'POST':
        form = forms.CityForm(request.POST)

        if form.is_valid():
            city = form.save(commit=False)
            city.save()

            return HttpResponseRedirect('/')
    else:
        form = forms.CityForm()

    return render(request, 'city_form.html', {'form': form})


def submit_styleurl(request):
    if request.method == 'POST':
        form = forms.StyleURLForm(request.POST)

        if form.is_valid():
            styleurl = form.save(commit=False)
            styleurl.save()

            return HttpResponseRedirect('/')
    else:
        form = forms.StyleURLForm()

    return render(request, 'styleurl_form.html', {'form': form})


def submit_droppoint(request):
    if request.method == 'POST':
        form = forms.DropPointForm(request.POST)

        if form.is_valid():
            droppoint = form.save(commit=False)
            droppoint.save()
            create_kml(droppoint, "droppoint", "create")
            #sync_kmls_file()
            #sync_kmls_to_galaxy()

            return HttpResponseRedirect('/droppoints/')
    else:
        form = forms.DropPointForm()

    return render(request, 'droppoint_form.html', {'form': form})


def submit_drone(request):
    if request.method == 'POST':
        form = forms.DroneForm(request.POST)

        if form.is_valid():
            drone = form.save(commit=False)
            drone.save()
            return HttpResponseRedirect('/hangars/')
    else:
        form = forms.DroneForm()

        return render(request, 'drone_form.html', {'form': form})


def submit_hangar(request):
    if request.method == 'POST':
        form = forms.HangarForm(request.POST)

        if form.is_valid():
            hangar = form.save(commit=False)
            hangar.drone.origin_lat = hangar.latitude
            hangar.drone.origin_lon = hangar.longitude
            # drone.altitude = altitude
            hangar.drone.save()
            hangar.save()
            create_kml(hangar, "hangar", "create")
            #sync_kmls_file()
            #sync_kmls_to_galaxy()

            return HttpResponseRedirect('/hangars/')
    else:
        form = forms.HangarForm()

    return render(request, 'hangar_form.html', {'form': form})


def submit_meteostation(request):
    if request.method == 'POST':
        form = forms.MeteoStationForm(request.POST)
        if form.is_valid():
            meteostation = form.save(commit=False)
            meteostation.save()
            create_kml(meteostation, "meteo", "create")
            #sync_kmls_file()
            #sync_kmls_to_galaxy()
            return HttpResponseRedirect('/meteostations/')
    else:

        form = forms.MeteoStationForm()
    return render(request, 'meteostation_form.html', {'form': form})


# REST API
class HangarViewSet(viewsets.ModelViewSet):
    queryset = models.Hangar.objects.all()
    serializer_class = HangarSerializer


class DropPointViewSet(viewsets.ModelViewSet):
    queryset = models.DropPoint.objects.all()
    serializer_class = DropPointSerializer


class MeteoStationViewSet(viewsets.ModelViewSet):
    queryset = models.MeteoStation.objects.all()
    serializer_class = MeteoStationSerializer


# Delte items
def delete_hangar(request, id):
    hangar = Hangar.objects.get(pk=id)
    delete_kml(hangar.id, "hangar")
    hangar.delete()
    return HttpResponseRedirect('/hangars/')


def delete_droppoint(request, id):
    droppoint = DropPoint.objects.get(pk=id)
    delete_kml(droppoint.id, "droppoint")
    droppoint.delete()
    return HttpResponseRedirect('/droppoints/')


def delete_meteostation(request, id):
    meteostation = MeteoStation.objects.get(pk=id)
    delete_kml(meteostation.id, "meteo")
    meteostation.delete()
    return HttpResponseRedirect('/meteostations/')


# Edit items
def edit_styleurl(request, id):
    requested_styleurl = StyleURL.objects.get(pk=id)
    form = StyleURLForm(instance=requested_styleurl)
    if request.method == 'POST':
        form = StyleURLForm(request.POST, instance=requested_styleurl)
        if form.is_valid():
            styleurl = form.save(commit=False)
            styleurl.save()

            return HttpResponseRedirect('/')

    return render(request, 'styleurl_form.html', {'form': form})


def edit_drone(request, id):
    requested_drone = Drone.objects.get(pk=id)
    form = DroneForm(instance=requested_drone)
    if request.method == 'POST':
        form = DroneForm(request.POST, instance=requested_drone)
        if form.is_valid():
            drone = form.save(commit=False)
            drone.save()

            return HttpResponseRedirect('/hangars')

    return render(request, 'drone_form.html', {'form': form})


def edit_hangar(request, id):
    requested_hangar = Hangar.objects.get(pk=id)
    form = HangarForm(instance=requested_hangar)
    if request.method == 'POST':
        form = HangarForm(request.POST, instance=requested_hangar)
        if form.is_valid():
            hangar = form.save(commit=False)
            hangar.drone.origin_lat = hangar.latitude
            hangar.drone.origin_lon = hangar.longitude
            # drone.altitude = altitude
            hangar.drone.save()
            hangar.save()
            create_kml(hangar, "hangar", "edit")

            #sync_kmls_file()
            #sync_kmls_to_galaxy()

            return HttpResponseRedirect('/hangars')

    return render(request, 'hangar_form.html', {'form': form})


def edit_meteostation(request, id):
    requested_meteo = MeteoStation.objects.get(pk=id)
    form = MeteoStationForm(instance=requested_meteo)
    if request.method == 'POST':
        form = MeteoStationForm(request.POST, instance=requested_meteo)
        if form.is_valid():
            meteostation = form.save(commit=False)
            meteostation.save()
            create_kml(meteostation, "meteo", "edit")
            #sync_kmls_file()
            #sync_kmls_to_galaxy()
            return HttpResponseRedirect('/meteostations')

    return render(request, 'meteostation_form.html', {'form': form})


def edit_droppoint(request, id):
    requested_droppoint = DropPoint.objects.get(pk=id)
    form = DropPointForm(instance=requested_droppoint)
    if request.method == 'POST':
        form = DropPointForm(request.POST, instance=requested_droppoint)
        if form.is_valid():
            droppoint = form.save(commit=False)
            droppoint.save()
            create_kml(droppoint, "droppoint", "edit")
            #sync_kmls_file()
            #sync_kmls_to_galaxy()

            return HttpResponseRedirect('/droppoints')

    return render(request, 'droppoint_form.html', {'form': form})


# Support functions
def create_kml(item, type, action):
    name = type + "_" + str(item.id) + ".kml"
    path = os.path.dirname(__file__) + "/static/kml/" + name

    if type == 'hangar':
        kml_generator.create_hangar_polygon(item, path)
    else:
        kml_generator.create_droppoint_marker(item, path)

    if action == 'create':
        Kml(name=name, url="static/kml/" + name).save()
    else:
        kml_vis = Kml.objects.get(name=name)
        kml_vis.visibility = item.is_available
        kml_vis.save()

    if type == 'hangar':
        name_influence = kml_generator.hangar_influence(item, os.path.dirname(__file__) + "/static/kml/" + type + "_" + str(item.id) + "_inf.kml")
        if action == 'create':
            Kml(name=name_influence, url="static/kml/" + name_influence, visibility=item.is_available).save()
        else:
            kml_vis = Kml.objects.get(name=name_influence)
            kml_vis.visibility = item.is_available
            kml_vis.save()


def delete_kml(id, type):
    filename = type + "_" + str(id) + ".kml"
    path = os.path.dirname(__file__) + "/static/kml/"
    for files in os.walk(path):
        if filename in files[2]:
            Kml.objects.get(name=filename).delete()
            os.remove(path + filename)
            if type == 'hangar':
                filename_inf = type + "_" + str(id) + "_inf.kml"
                if filename_inf in files[2]:
                    Kml.objects.get(name=filename_inf).delete()
                    os.remove(path + filename_inf)

            #sync_kmls_file()
            #sync_kmls_to_galaxy()

            return


# Geo functions
def find_emergency_path(request):

    MAX_WIND_SPEED = 10.0

    url = 'http://api.openweathermap.org/data/2.5/weather?q=Lleida&units=metric'
    response = requests.get(url=url)
    data = json.loads(response.text)

    try:
        if data['wind']['speed'] >= MAX_WIND_SPEED or bool(data['rain']):
            print data['rain']
            print data['wind']['speed']
            return HttpResponse(status=503)
    except KeyError:
        pass

    lat = request.GET.get('lat', '')
    lon = request.GET.get('lng', '')
    path = os.path.dirname(__file__) + "/static/kml/"

    last_distance = sys.maxint
    all_hangars = models.Hangar.objects.all()
    selected_hangar = None
    all_droppoints = models.DropPoint.objects.all()
    selected_droppoint = None
    point_location = Point(float(lon), float(lat))

    for droppoint in all_droppoints:
        distance = D(m=point_location.distance(Point(droppoint.longitude, droppoint.latitude)))
        if distance.m < last_distance:
            last_distance = distance.m
            selected_droppoint = droppoint

    last_distance = sys.maxint
    point_location = Point(selected_droppoint.longitude, selected_droppoint.latitude)
    for hangar in all_hangars:
        distance = D(m=point_location.distance(Point(hangar.longitude, hangar.latitude)))
        if distance.m < last_distance:
            last_distance = distance.m
            selected_hangar = hangar

    # print selected_hangar.name, selected_hangar.drone.name, selected_droppoint.name

    selected_hangar.drone.destination_lat = selected_droppoint.latitude
    selected_hangar.drone.destination_lon = selected_droppoint.longitude

    if not selected_hangar.is_available:
        return HttpResponse(status=503)

    kml_generator.weather_info(os.path.dirname(__file__) + "/static/kml/meteo_info.kml",
                               data['main']['temp'], data['main']['temp_max'], data['main']['temp_min'],
                               data['wind']['speed'], data['clouds']['all'], data['main']['pressure'],
                               data['main']['humidity'], data['weather'][0]['description'])

    # generate_mission_file(selected_hangar)
    kml_generator.create_emergency_marker(lat, lon, path + "incidence.kml")
    Kml(name="incidence.kml", url="static/kml/incidence.kml", visibility=True).save()
    #sync_kmls_file()
    #sync_kmls_to_galaxy(emergency=True)
    kml_generator.find_drone_path(selected_hangar, selected_droppoint, path)

    Kml.objects.get(name="incidence.kml").delete()
    os.remove(path + "incidence.kml")

    for step in range(0, 34, 1):
        Kml.objects.get(name="drone_" + str(step) + ".kml").delete()
        os.remove(path + "drone_" + str(step) + ".kml")

    #sync_kmls_file()
    #sync_kmls_to_galaxy(emergency=True)

    return HttpResponse(status=201)

def generate_mission_file(hangar):
    with open("/home/lg/interactivespaces/controller/controller/activities/installed/f04abff8-1bd3-4c9e-8366-8f1b44f05cb5/install", "w") as mission_file:
        mission_file.write("QGC WPL 110\n"
                          + "0\t0\t0\t16\t0.000000\t0.000000\t0.000000\t0.000000\t" + str(hangar.longitude) + "\t" + str(hangar.latitude) + "\t" + str(hangar.altitude) + "\t1\n"
                          + "1\t0\t3\t22\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t50.000000\t1\n"
                          + "2\t0\t3\t16\t0.000000\t0.000000\t0.000000\t0.000000\t" + str(hangar.drone.destination_lon) + "\t" + str(hangar.drone.destination_lat) + "\t" + str(hangar.altitude) + "\t1\n"
                          + "3\t0\t3\t21\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t0.000000\t1\n")
