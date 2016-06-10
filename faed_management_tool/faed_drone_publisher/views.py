from faed_management import models
from django.shortcuts import render
from django.http import Http404


def drone_detail(request, drone_plate):
    try:
        drone = models.Drone.objects.get(plate=drone_plate)
        print drone
    except drone is None:
        raise Http404

    return render(request, 'drone.html', {'drone': drone})
