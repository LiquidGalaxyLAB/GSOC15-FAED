import math
import time
import random

import geopy
import simplekml
from polycircles import polycircles
from geopy.distance import VincentyDistance

from kmls_management.models import Kml
from faed_management.static.py_func.sendtoLG import sync_kmls_to_galaxy, sync_kmls_file

from faed_management.static.py_func.sendtoLG import get_server_ip

kml_path = "/static/kml"

def placemark_kml(drone, geo_point, filename):
    with open(filename, "w") as kml_file:
        kml_file.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                       + "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"
                       + "\t<Document>\n"
                       + "\t\t<Style id=\"drone\">\n"
                       + "\t\t\t<IconStyle>\n"
                       + "\t\t\t\t<Icon>\n"
                       + "\t\t\t\t\t<href>" + drone.style_url.href + "</href>\n"
                       + "\t\t\t\t\t<scale>1.0</scale>\n"
                       + "\t\t\t\t</Icon>\n"
                       + "\t\t\t</IconStyle>\n"
                       + "\t\t</Style>\n"
                       + "\t\t<Placemark>\n"
                       # + "\t\t\t<name>" + drone.name + "</name>\n"
                       + "\t\t\t<description>Drone covering emergency</description>\n"
                       + "\t\t\t<styleUrl>#drone</styleUrl>\n"
                       + "\t\t\t<Point>\n"
                       + "\t\t\t\t<altitudeMode>relativeToGround</altitudeMode>\n"
                       + "\t\t\t\t<coordinates>"
                       + str(geo_point[1]) + "," + str(geo_point[0]) + "," + str(geo_point[2])
                       + "</coordinates>\n"
                       + "\t\t\t</Point>\n"
                       + "\t\t</Placemark>\n"
                       + "\t</Document>\n"
                       + "</kml>")


def manage_dron_route_kml(filename, url):
    with open(filename+".kml", "w") as kml_file:
        kml_file.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                       + "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"
                       + "\t<Document>\n"
                       + "\t\t<NetworkLink>\n"
                       + "\t\t\t<Link>\n"
                       + "\t\t\t\t<href>" + url + "</href>\n"
                       + "\t\t\t\t<refreshMode>onInterval</refreshMode>\n"
                       + "\t\t\t\t<refreshInterval>1</refreshInterval>\n"
                       + "\t\t\t</Link>\n"
                       + "\t\t</NetworkLink>\n"
                       + "\t</Document>\n"
                       + "</kml>")


def circle_kml(points, filename):
    with open(filename, "w") as kml_file:
        kml_file.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                       + "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"
                       + "\t<Document>\n"
                       + "\t\t<name>Influence Radius</name>\n"
                       + "\t\t<visibility>1</visibility>\n"
                       + "\t\t<Placemark>\n"
                       + "\t\t\t<name>Hangar</name>\n"
                       + "\t\t\t<visibility>1</visibility>\n"
                       + "\t\t\t<Style>\n"
                       + "\t\t\t\t<LineStyle>\n"
                       + "\t\t\t\t\t<color>" + random_color() + "</color>\n"
                       + "\t\t\t\t\t<scale>1</scale>\n"
                       + "\t\t\t\t\t<width>10</width>\n"
                       + "\t\t\t\t</LineStyle>\n"
                       + "\t\t\t</Style>\n"
                       + "\t\t\t<LineString>\n"
                       + "\t\t\t\t<altitudeMode>absolute</altitudeMode>\n"
                       + "\t\t\t\t<coordinates>\n")
        for p in points:
            kml_file.write("\t\t\t\t" + p + "\n")

        kml_file.write("\t\t\t\t</coordinates>\n"
                       + "\t\t\t</LineString>\n"
                       + "\t\t</Placemark>\n"
                       + "\t</Document>\n"
                       + "</kml>")


def create_hangar_polygon(hangar, filename):
    with open(filename, "w") as kml_file:
        kml_file.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                       + "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"
                       + "\t<Document>\n"
                       + "\t\t<Style id=\"Red\">\n"
                       + "\t\t\t<LineStyle>\n"
                       + "\t\t\t\t<width>1.5</width>\n"
                       + "\t\t\t</LineStyle>\n"
                       + "\t\t\t<PolyStyle>\n"
                       + "\t\t\t\t<color>0ff000000</color>\n"
                       + "\t\t\t</PolyStyle>\n"
                       + "\t\t</Style>\n"
                       + "\t\t<Placemark>\n"
                       + "\t\t\t<name>" + hangar.name + "</name>\n"
                       + "\t\t\t<description>" + hangar.description + "</description>\n"
                       + "\t\t\t<styleUrl>#Red</styleUrl>\n"
                       + "\t\t\t<Polygon>\n"
                       + "\t\t\t\t<extrude>1</extrude>\n"
                       + "\t\t\t\t<altitudeMode>relativeToGround</altitudeMode>\n"
                       + "\t\t\t\t<outerBoundaryIs>\n"
                       + "\t\t\t\t\t<LinearRing>\n"
                       + "\t\t\t\t\t\t<coordinates>\n"
                       + "\t\t\t\t\t\t\t" + str(hangar.longitude) + "," + str(hangar.latitude) + "," + str(
            hangar.altitude) + "\n"
                       + "\t\t\t\t\t\t\t" + str(hangar.longitude) + "," + str(hangar.latitude + 0.0001) + "," + str(
            hangar.altitude) + "\n"
                       + "\t\t\t\t\t\t\t" + str(hangar.longitude + 0.0001) + "," + str(
            hangar.latitude + 0.0001) + "," + str(hangar.altitude) + "\n"
                       + "\t\t\t\t\t\t\t" + str(hangar.longitude + 0.0001) + "," + str(hangar.latitude) + "," + str(
            hangar.altitude) + "\n"
                       + "\t\t\t\t\t\t\t" + str(hangar.longitude) + "," + str(hangar.latitude) + "," + str(
            hangar.altitude) + "\n"
                       + "\t\t\t\t\t\t</coordinates>\n"
                       + "\t\t\t\t\t</LinearRing>\n"
                       + "\t\t\t\t</outerBoundaryIs>\n"
                       + "\t\t\t</Polygon>\n"
                       + "\t\t</Placemark>\n"
                       + "\t</Document>\n"
                       + "</kml>\n")


def hangar_influence(hangar, path):
    print hangar.name, hangar.id
    name = "hangar_" + str(hangar.id) + "_inf.kml"
    print name

    polycircle = polycircles.Polycircle(latitude=hangar.latitude,
                                        longitude=hangar.longitude,
                                        radius=hangar.radius,
                                        number_of_vertices=36)
    points_list = polycircle.to_lat_lon()
    latlonalt = []
    for tuple in points_list:
        tup = (tuple[1], tuple[0], 5)
        latlonalt.append(tup)

    kml = simplekml.Kml(open=1)
    shape_polycircle = kml.newmultigeometry(name=hangar.name)
    pol = shape_polycircle.newpolygon()
    pol.outerboundaryis = latlonalt

    pol.altitudemode = simplekml.AltitudeMode.relativetoground
    pol.extrude = 5
    pol.style.polystyle.color = '22ff0000'
    pol.style.polystyle.fill = 1
    pol.style.polystyle.outline = 1
    pol.style.linestyle.width = 10
    pol.style.linestyle.color = simplekml.Color.red

    '''
    pol = kml.newpolygon(name=hangar.description, outerboundaryis=polycircle.to_kml())
    pol.style.polystyle.color = simplekml.Color.changealphaint(200, simplekml.Color.darksalmon)
    '''
    kml.save(path)

    return name


def create_droppoint_marker(placemark, filename):
    with open(filename, "w") as kml_file:
        kml_file.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                       + "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"
                       + "\t<Placemark>\n"
                       + "\t\t<name>" + placemark.name + "</name>\n"
                       + "\t\t<visibility>1</visibility>\n"
                       + "\t\t<description>" + placemark.description + "</description>\n"
                       + "\t\t<Style>\n"
                       + "\t\t\t<IconStyle>\n"
                       + "\t\t\t\t<Icon>\n"
                       + "\t\t\t\t\t<href>" + placemark.style_url.href + "</href>\n"
                       + "\t\t\t\t</Icon>\n"
                       + "\t\t\t</IconStyle>\n"
                       + "\t\t\t<LineStyle>\n"
                       + "\t\t\t\t<width>2</width>\n"
                       + "\t\t\t</LineStyle>\n"
                       + "\t\t</Style>\n"
                       + "\t\t<Point>\n"
                       + "\t\t\t<extrude>1</extrude>\n"
                       + "\t\t\t<altitudeMode>relativeToGround</altitudeMode>\n"
                       + "\t\t\t<coordinates>"
                       + str(placemark.longitude) + "," + str(placemark.latitude) + "," + str(placemark.altitude)
                       + "</coordinates>\n"
                       + "\t\t</Point>\n"
                       + "\t</Placemark>\n"
                       + "</kml>")


def create_emergency_marker(latitude, longitude, filename):
    with open(filename, "w") as kml_file:
        kml_file.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                       + "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"
                       + "\t<Placemark>\n"
                       + "\t\t<name>FAED</name>\n"
                       + "\t\t<visibility>1</visibility>\n"
                       + "\t\t<description>Incidence</description>\n"
                       + "\t\t<Style>\n"
                       + "\t\t\t<IconStyle>\n"
                       + "\t\t\t\t<Icon>\n"
                       + "\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/shapes/caution.png</href>\n"
                       + "\t\t\t\t\t<scale>100</scale>\n"
                       + "\t\t\t\t</Icon>\n"
                       + "\t\t\t</IconStyle>\n"
                       + "\t\t\t<LineStyle>\n"
                       + "\t\t\t\t<width>2</width>\n"
                       + "\t\t\t</LineStyle>\n"
                       + "\t\t</Style>\n"
                       + "\t\t<Point>\n"
                       + "\t\t\t<extrude>1</extrude>\n"
                       + "\t\t\t<altitudeMode>relativeToGround</altitudeMode>\n"
                       + "\t\t\t<coordinates>" + str(longitude) + "," + str(latitude) + "," + str(200)
                       + "</coordinates>\n"
                       + "\t\t</Point>\n"
                       + "\t</Placemark>\n"
                       + "</kml>")


def calculate_initial_compass_bearing(geo_point_origin, geo_point_destiny):
    if (type(geo_point_origin) != tuple) or (type(geo_point_destiny) != tuple):
        raise TypeError("Only tuples are supported as arguments")

    lat1 = math.radians(geo_point_origin[0])
    lat2 = math.radians(geo_point_destiny[0])

    diffLong = math.radians(geo_point_destiny[1] - geo_point_origin[1])

    x = math.sin(diffLong) * math.cos(lat2)
    y = math.cos(lat1) * math.sin(lat2) - (math.sin(lat1)
                                           * math.cos(lat2) * math.cos(diffLong))

    initial_bearing = math.atan2(x, y)

    initial_bearing = math.degrees(initial_bearing)
    compass_bearing = (initial_bearing + 360) % 360

    return compass_bearing


def find_drone_path(hangar, droppoint, path_name, id_incidence, is_returning=False):
    geo_point_origin = (hangar.latitude, hangar.longitude)
    geo_point_destiny = (droppoint.latitude, droppoint.longitude)
    file_path = path_name + "in" + str(id_incidence) + "drone.kml"
    dist = geopy.distance.distance(geo_point_origin, geo_point_destiny).kilometers
    ip_server = get_server_ip()
    url_file = "http://" + str(ip_server)[0:(len(ip_server) - 1)] + ":8000/static/kml/" + "in" + str(id_incidence) + "drone.kml" + "\n"
    bearing = calculate_initial_compass_bearing(geo_point_origin, geo_point_destiny)
    manage_dron_route_kml(path_name + "manage" + str(id_incidence), url_file)
    drone_to_galaxy()
    print file_path
    print url_file
    # time.sleep(5)
    steps = 20
    num_steps = steps
    step = 0
    altitude = hangar.altitude

    # Drone taking off
    while step <= 6:
        placemark_kml(hangar.drone,
                      (geo_point_origin[0], geo_point_origin[1], altitude), file_path)
        # Kml(name=file_name, url=kml_path+file_name, visibility=True).save()
        altitude += 25
        step += 1
        time.sleep(1)
        # drone_to_galaxy()

    lat = geo_point_origin[0]
    lon = geo_point_origin[1]
    placemark_kml(hangar.drone, (geo_point_origin[0], geo_point_origin[1], altitude), file_path)
    # Kml(name=kml_path+file_name, url=kml_path+file_name, visibility=True).save()
    step += 1
    time.sleep(1)
    # drone_to_galaxy()

    # Drone route
    while num_steps > 0:
        destination = VincentyDistance(kilometers=dist / steps).destination(geopy.Point(lat, lon), bearing)
        lat, lon = destination.latitude, destination.longitude
        placemark_kml(hangar.drone, (lat, lon, altitude), file_path)
        # Kml(name="drone_" + str(step) + ".kml", url="static/kml/drone_" + str(step) + ".kml", visibility=True).save()
        num_steps -= 1
        step += 1
        time.sleep(1)
        # drone_to_galaxy()

    # Drone landing
    while step <= 33:
        placemark_kml(hangar.drone, (lat, lon, altitude), file_path)
        # Kml(name="drone_" + str(step) + ".kml", url="static/kml/drone_" + str(step) + ".kml", visibility=True).save()
        altitude -= 25
        step += 1
        time.sleep(1)
        # drone_to_galaxy()

    print "Finished"
    # time.sleep(5)
    # if not is_returning:
        #Make incidence clear or something like this
        # find_drone_path(droppoint, hangar, path_name, id_incidence, True)


def drone_to_galaxy():
    sync_kmls_file()
    sync_kmls_to_galaxy(emergency=True)
    time.sleep(1.5)


def weather_info(filename, temp, temp_max, temp_min, wind, cloud, pressure, humidity, description):
    with open(filename, "w") as kml_file:
        kml_file.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                       + "<kml xmlns=\"http://www.opengis.net/kml/2.2\"\n"
                       + "xmlns:gx=\"http://www.google.com/kml/ext/2.2\">\n"
                       + "\t<Placemark>\n"
                       + "\t\t<name>Weather Station Lleida</name>\n"
                       + "\t\t<gx:balloonVisibility>1</gx:balloonVisibility>\n"
                       + "\t\t<description><![CDATA[\n"
                       + "\t\t\t<p><font size=5><b>Temperature</b> - " + str(temp) + "C</font></p>\n"
                       + "\t\t\t<p><font size=5 color=\"blue\"><b>Temperature max. - " + str(
            temp_max) + "C</b></font></p>\n"
                       + "\t\t\t<p><font size=5 color=\"red\"><b>Temperature min. - " + str(
            temp_min) + "</b></font></p>\n"
                       + "\t\t\t<p><font size=5><b>Wind - " + str(wind) + "m/s</b></font></p>\n"
                       + "\t\t\t<p><font size=5><b>Clouds - " + str(cloud) + "%</b></font></p>\n"
                       + "\t\t\t<p><font size=5><b>Pressure - " + str(pressure) + " hpa</b></font></p>\n"
                       + "\t\t\t<p><font size=5><b>Humidity - " + str(humidity) + "%</b></font></p>\n"
                       + "\t\t\t<hr>\n"
                       + "\t\t\tLocation:<br>\n"
                       + "\t\t\t<p><font size=5><b>Latitude - 41.62</b></font></p>\n"
                       + "\t\t\t<p><font size=5><b>Longitude - 0.62</b></font></p>\n"
                       + "\t\t\t<p><font size=5><b>Sky status - " + description + "</b></font></p>\n"
                       # Avoid scrolling
                       + "\t\t\t<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</p>\n"
                       + "\t\t\t<hr>\n"
                       + "\t\t\t</br>\n"
                       + "\t\t\t<font face=arial>Arial</font><br>\n"
                       + "\t\t]]></description>\n"
                       + "\t\t<Point>\n"
                       + "\t\t\t<coordinates>-122.0822035425683,37.42228990140251,0</coordinates>\n"
                       + "\t\t</Point>\n"
                       + "\t</Placemark>\n"
                       + "</kml>")


def random_color():
    r = lambda: random.randint(0, 255)
    return '%02X%02X%02X%02X' % (r(), r(), r(), r())
