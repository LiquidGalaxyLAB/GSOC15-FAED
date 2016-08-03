import math
import time
import random
import os
import geopy
import simplekml
from polycircles import polycircles
from geopy.distance import VincentyDistance

from kmls_management.models import Kml
from faed_management.static.py_func.sendtoLG import sync_kmls_to_galaxy, \
    sync_kmls_file, get_server_ip

kml_path = "/static/kml"


def placemark_kml(drone, geo_point, filename):
    ip_server = get_server_ip()
    pic_url = "http://" + str(ip_server)[0:(len(ip_server) - 1)] + \
              ":8000/static/img/ICON_LG_FAED.png"
    with open(filename, "w") as kml_file:
        kml_file.write(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
            "\t<Document>\n" +
            "\t\t<Style id=\"drone\">\n" +
            "\t\t\t<IconStyle>\n" +
            "\t\t\t\t<Icon>\n" +
            "\t\t\t\t\t<href>" + pic_url + "</href>\n" +
            "\t\t\t\t\t<scale>1.0</scale>\n" +
            "\t\t\t\t</Icon>\n" +
            "\t\t\t</IconStyle>\n" +
            "\t\t</Style>\n" +
            "\t\t<Placemark>\n" +
            "\t\t\t<name>" + drone.name + "</name>\n" +
            "\t\t\t<description>Drone covering emergency</description>\n" +
            "\t\t\t<styleUrl>#drone</styleUrl>\n" +
            "\t\t\t<Point>\n" +
            "\t\t\t\t<altitudeMode>relativeToGround</altitudeMode>\n" +
            "\t\t\t\t<coordinates>" +
            str(geo_point[1]) + "," + str(geo_point[0]) + "," +
            str(geo_point[2]) +
            "</coordinates>\n" +
            "\t\t\t</Point>\n" +
            "\t\t</Placemark>\n" +
            "\t</Document>\n" +
            "</kml>")


def manage_kml(filename, url, refresh_time):
    with open(filename + ".kml", "w") as kml_file:
        kml_file.write(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
            "\t<Document>\n" +
            "\t\t<NetworkLink>\n" +
            "\t\t\t<Link>\n" +
            "\t\t\t\t<href>" + url + "</href>\n" +
            "\t\t\t\t<refreshMode>onInterval</refreshMode>\n" +
            "\t\t\t\t<refreshInterval>" + str(refresh_time) +
            "</refreshInterval>\n" +
            "\t\t\t</Link>\n" +
            "\t\t</NetworkLink>\n" +
            "\t</Document>\n" +
            "</kml>")


'''
    At the moment this function reloads every X time, if there are more
    incidences can be changed and delete the refresh time.
'''


def faed_logo_kml(filename, url, p1, p2, sizex, sizey):
    with open(filename + ".kml", "w") as kml_file:
        kml_file.write(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
            "\t<Document>\n" +
            "\t\t<ScreenOverlay>\n" +
            "\t\t\t<Icon>\n" +
            "\t\t\t\t<href>" + url + "</href>\n" +
            "\t\t\t</Icon>\n" +
            "\t\t\t<overlayXY x='0' y='" + str(p1) + "' xunits='fraction' " +
            "yunits='fraction'/>\n" +
            "\t\t\t<screenXY x='0' y='" + str(p2) + "' xunits='fraction' " +
            "yunits='fraction'/>\n" +
            "\t\t\t<rotationXY x='0' y='0' xunits='fraction' " +
            "yunits='fraction'/>\n" +
            "\t\t\t<size x='" + str(sizex) + "' y='" + str(sizey) +
            "' xunits='fraction' yunits='fraction'/>\n" +
            "\t\t</ScreenOverlay>\n" +
            "\t</Document>\n" +
            "</kml>")


def circle_kml(points, filename):
    with open(filename, "w") as kml_file:
        kml_file.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                       "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                       "\t<Document>\n" +
                       "\t\t<name>Influence Radius</name>\n" +
                       "\t\t<visibility>1</visibility>\n" +
                       "\t\t<Placemark>\n" +
                       "\t\t\t<name>Hangar</name>\n" +
                       "\t\t\t<visibility>1</visibility>\n" +
                       "\t\t\t<Style>\n" +
                       "\t\t\t\t<LineStyle>\n" +
                       "\t\t\t\t\t<color>" + random_color() + "</color>\n" +
                       "\t\t\t\t\t<scale>1</scale>\n" +
                       "\t\t\t\t\t<width>10</width>\n" +
                       "\t\t\t\t</LineStyle>\n" +
                       "\t\t\t</Style>\n" +
                       "\t\t\t<LineString>\n" +
                       "\t\t\t\t<altitudeMode>absolute</altitudeMode>\n" +
                       "\t\t\t\t<coordinates>\n")
        for p in points:
            kml_file.write("\t\t\t\t" + p + "\n")

        kml_file.write("\t\t\t\t</coordinates>\n" +
                       "\t\t\t</LineString>\n" +
                       "\t\t</Placemark>\n" +
                       "\t</Document>\n" +
                       "</kml>")


def create_general(filename, droppoints):
    ip_server = get_server_ip()
    url = "http://" + str(ip_server)[0:(len(ip_server) - 1)] + \
                ":8000/static/kml/"
    with open(filename, "w") as kml_file:
        kml_file.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                       "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                       "\t<Document>\n")
        for dp in droppoints:
            kml_file.write("\t\t<NetworkLink>\n" +
                           "\t\t\t<Name>" + dp + "</Name>\n"
                           "\t\t<Link>\n"
                           "\t\t<href>" + url + dp + "</href>\n"
                           "\t\t</Link>\n"
                           "\t\t</NetworkLink>\n")
        kml_file.write("\t</Document>\n" +
                       "</kml>"
                       )


def create_hangar_polygon(hangar, filename):
    with open(filename, "w") as kml_file:
        kml_file.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                       "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                       "\t<Document>\n" +
                       "\t\t<Style id=\"Red\">\n" +
                       "\t\t\t<LineStyle>\n" +
                       "\t\t\t\t<width>1.5</width>\n" +
                       "\t\t\t</LineStyle>\n" +
                       "\t\t\t<PolyStyle>\n" +
                       "\t\t\t\t<color>0ff000000</color>\n" +
                       "\t\t\t</PolyStyle>\n" +
                       "\t\t</Style>\n" +
                       "\t\t<Placemark>\n" +
                       "\t\t\t<name>" + hangar.name + "</name>\n" +
                       "\t\t\t<description>" + hangar.description +
                       "</description>\n" +
                       "\t\t\t<styleUrl>#Red</styleUrl>\n" +
                       "\t\t\t<Polygon>\n" +
                       "\t\t\t\t<extrude>1</extrude>\n" +
                       "\t\t\t\t<altitudeMode>relativeToGround" +
                       "</altitudeMode>\n" +
                       "\t\t\t\t<outerBoundaryIs>\n" +
                       "\t\t\t\t\t<LinearRing>\n" +
                       "\t\t\t\t\t\t<coordinates>\n" +
                       "\t\t\t\t\t\t\t" + str(hangar.longitude) + "," +
                       str(hangar.latitude) + "," + str(hangar.altitude) +
                       "\n" +
                       "\t\t\t\t\t\t\t" + str(hangar.longitude) + "," +
                       str(hangar.latitude + 0.0001) + "," +
                       str(hangar.altitude) + "\n" +
                       "\t\t\t\t\t\t\t" + str(hangar.longitude + 0.0001) +
                       "," + str(hangar.latitude + 0.0001) +
                       "," + str(hangar.altitude) + "\n" +
                       "\t\t\t\t\t\t\t" + str(hangar.longitude + 0.0001) +
                       "," + str(hangar.latitude) + "," +
                       str(hangar.altitude) + "\n" +
                       "\t\t\t\t\t\t\t" + str(hangar.longitude) + "," +
                       str(hangar.latitude) + "," + str(hangar.altitude) +
                       "\n" +
                       "\t\t\t\t\t\t</coordinates>\n" +
                       "\t\t\t\t\t</LinearRing>\n" +
                       "\t\t\t\t</outerBoundaryIs>\n" +
                       "\t\t\t</Polygon>\n" +
                       "\t\t</Placemark>\n" +
                       "\t</Document>\n" +
                       "</kml>\n")


def hangar_influence(hangar, path):
    name = "hangar_" + str(hangar.id) + "_inf.kml"

    polycircle = polycircles.Polycircle(latitude=hangar.latitude,
                                              longitude=hangar.longitude,
                                              radius=hangar.radius,
                                              number_of_vertices=36)
    # kml = simplekml.Kml()
    points_list = polycircle.to_lat_lon()
    latlonalt = []
    for idx, points_tuple in enumerate(points_list):
        if idx == 0:
            first_point = (points_tuple[1], points_tuple[0])
        tup = (points_tuple[1], points_tuple[0])
        latlonalt.insert(0, tup)
    latlonalt.insert(0, first_point)
    #
    kml = simplekml.Kml(open=1)
    shape_polycircle = kml.newmultigeometry(name=hangar.name)
    pol = shape_polycircle.newpolygon()
    # pol = kml.newpolygon(name=name) # /
    pol.outerboundaryis = latlonalt # /
    # Line Style
    # pol.style.linestyle.color = simplekml.Color.red # /
    # pol.style.linestyle.width = 20 # /
    # If you want to see the filled polygon needs to enable relative to ground
    pol.style.polystyle.fill = 1
    pol.style.polystyle.outline = 1
    pol.style.polystyle.color = simplekml.Color.changealphaint(100, simplekml.Color.blue)
    pol.altitudemode = simplekml.AltitudeMode.relativetoground
    pol.extrude = 5
    pol.style.polystyle.color = '22ff0000'
    pol.style.polystyle.fill = 1
    pol.style.polystyle.outline = 1
    pol.style.linestyle.width = 20
    pol.style.linestyle.color = simplekml.Color.red

    '''
    pol = kml.newpolygon(name=hangar.description,
                         outerboundaryis=polycircle.to_kml())
    pol.style.polystyle.color =
    simplekml.Color.changealphaint(200, simplekml.Color.darksalmon)
    '''
    kml.save(path)

    return name


def create_droppoint_marker(placemark, filename):
    with open(filename, "w") as kml_file:
        kml_file.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                       "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                       "\t<Placemark>\n" +
                       "\t\t<name>" + placemark.name + "</name>\n" +
                       "\t\t<visibility>1</visibility>\n" +
                       "\t\t<description>" + placemark.description +
                       "</description>\n" +
                       "\t\t<Style>\n" +
                       "\t\t\t<IconStyle>\n" +
                       "\t\t\t\t<Icon>\n" +
                       "\t\t\t\t\t<href>" + placemark.style_url.href +
                       "</href>\n" +
                       "\t\t\t\t</Icon>\n" +
                       "\t\t\t</IconStyle>\n" +
                       "\t\t\t<LineStyle>\n" +
                       "\t\t\t\t<width>2</width>\n" +
                       "\t\t\t</LineStyle>\n" +
                       "\t\t</Style>\n" +
                       "\t\t<Point>\n" +
                       "\t\t\t<extrude>1</extrude>\n" +
                       "\t\t\t<altitudeMode>relativeToGround" +
                       "</altitudeMode>\n" +
                       "\t\t\t<coordinates>" +
                       str(placemark.longitude) + "," +
                       str(placemark.latitude) + "," +
                       str(placemark.altitude) + "</coordinates>\n" +
                       "\t\t</Point>\n" +
                       "\t</Placemark>\n" +
                       "</kml>")


def create_emergency_marker(incidence, filename, resolved=False):
    if not resolved:
        description = "Emergency"
        url_image = "http://maps.google.com/mapfiles/kml/shapes/caution.png"
    else:
        description = "Resolved"
        ip_server = get_server_ip()
        url_image = "http://" + str(ip_server)[0:(len(ip_server) - 1)] + \
                    ":8000/static/img/green_cross.png"
    with open(filename, "w") as kml_file:
        kml_file.write(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
            "\t<Placemark>\n" +
            "\t\t<name>FAED</name>\n" +
            "\t\t<visibility>1</visibility>\n" +
            "\t\t<description>" + description + "</description>\n" +
            "\t\t<Style>\n" +
            "\t\t\t<IconStyle>\n" +
            "\t\t\t\t<Icon>\n" +
            "\t\t\t\t\t<href>" + url_image + "</href>\n" +
            "\t\t\t\t\t<scale>100</scale>\n" +
            "\t\t\t\t</Icon>\n" +
            "\t\t\t</IconStyle>\n" +
            "\t\t\t<LineStyle>\n" +
            "\t\t\t\t<width>2</width>\n" +
            "\t\t\t</LineStyle>\n" +
            "\t\t</Style>\n" +
            "\t\t<Point>\n" +
            "\t\t\t<extrude>1</extrude>\n" +
            "\t\t\t<altitudeMode>relativeToGround" +
            "</altitudeMode>\n" +
            "\t\t\t<coordinates>" + str(incidence.long) + "," +
            str(incidence.lat) + "," + str(200) +
            "</coordinates>\n" +
            "\t\t</Point>\n" +
            "\t</Placemark>\n" +
            "</kml>")


def calculate_initial_compass_bearing(geo_point_origin, geo_point_destiny):
    if (type(geo_point_origin) != tuple) or (type(geo_point_destiny) != tuple):
        raise TypeError("Only tuples are supported as arguments")

    lat1 = math.radians(geo_point_origin[0])
    lat2 = math.radians(geo_point_destiny[0])

    diff_long = math.radians(geo_point_destiny[1] - geo_point_origin[1])

    x = math.sin(diff_long) * math.cos(lat2)
    y = math.cos(lat1) * math.sin(lat2) - (math.sin(lat1) * math.cos(lat2) *
                                           math.cos(diff_long))

    initial_bearing = math.atan2(x, y)

    initial_bearing = math.degrees(initial_bearing)
    compass_bearing = (initial_bearing + 360) % 360

    return compass_bearing


def find_drone_path(origin, destiny, path_name, incidence, is_returning=False):
    geo_point_origin = (origin.latitude, origin.longitude)
    geo_point_destiny = (destiny.latitude, destiny.longitude)
    file_path = path_name + "in" + str(incidence.id) + "drone.kml"
    dist = geopy.distance.distance(geo_point_origin, geo_point_destiny) \
        .kilometers
    bearing = \
        calculate_initial_compass_bearing(geo_point_origin, geo_point_destiny)
    take_off_steps = 8
    altitude = origin.altitude
    Kml(name="incidence" + str(incidence.id) + ".kml",
        url=path_name + "incidence" + str(incidence.id) + ".kml",
        visibility=True).save()
    create_emergency_marker(incidence, path_name + "incidence" +
                            str(incidence.id) + ".kml")
    placemark_kml(incidence.drone,
                  (geo_point_origin[0], geo_point_origin[1], altitude),
                  file_path)
    take_off_steps -= 1
    altitude += 25
    sync_kml_galaxy()
    create_drone_manage_route(path_name, incidence.id)
    steps = 20
    lat = geo_point_origin[0]
    lon = geo_point_origin[1]

    for i in range(take_off_steps):
        placemark_kml(incidence.drone,
                      (geo_point_origin[0], geo_point_origin[1], altitude),
                      file_path)
        altitude += 25
        time.sleep(2)

    for i in range(steps):
        destination = VincentyDistance(kilometers=dist / steps) \
            .destination(geopy.Point(lat, lon), bearing)
        lat, lon = destination.latitude, destination.longitude
        placemark_kml(incidence.drone, (lat, lon, altitude), file_path)
        time.sleep(2)

    for i in range(8):
        placemark_kml(incidence.drone, (lat, lon, altitude), file_path)
        altitude -= 25
        time.sleep(2)

    time.sleep(5)
    os.remove(path_name + "incidence" + str(incidence.id) + ".kml")
    Kml.objects.filter(name="incidence" + str(incidence.id) + ".kml"). \
        delete()
    Kml(name="finishedincidence" + str(incidence.id) + ".kml",
        url=path_name + "finishedincidence" + str(
            incidence.id) + ".kml").save()
    create_emergency_marker(incidence, path_name + "finishedincidence" +
                            str(incidence.id) + ".kml", True)
    sync_kml_galaxy()
    time.sleep(10)
    delete_incidence(incidence, path_name)
    time.sleep(30)
    delete_finished_incidence_marker(incidence, path_name)


def sync_kml_galaxy():
    sync_kmls_file()
    sync_kmls_to_galaxy(emergency=True)
    time.sleep(1.5)


def delete_finished_incidence_marker(incidence, path):
    os.remove(path + "finishedincidence" + str(incidence.id) + ".kml")
    Kml.objects.filter(name="finishedincidence" + str(incidence.id) + ".kml") \
        .delete()
    sync_kml_galaxy()


def delete_incidence(incidence, path):
    incidence.is_active = False
    incidence.save()
    os.remove(path + "manage" + str(incidence.id) + ".kml")
    Kml.objects.filter(name="manage" + str(incidence.id) + ".kml").delete()
    os.remove(path + "in" + str(incidence.id) + "drone.kml")
    Kml.objects.filter(name="in" + str(incidence.id) + "drone.kml").delete()
    sync_kml_galaxy()


def weather_info(filename, temp, temp_max, temp_min, wind, cloud, pressure,
                 humidity, description):
    with open(filename, "w") as kml_file:
        kml_file.write(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\"\n" +
            "xmlns:gx=\"http://www.google.com/kml/ext/2.2\">\n" +
            "\t<Placemark>\n" +
            "\t\t<name>Weather Station Lleida</name>\n" +
            "\t\t<gx:balloonVisibility>1</gx:balloonVisibility>\n" +
            "\t\t<description><![CDATA[\n" +
            "\t\t\t<p><font size=5><b>Temperature</b> - " + str(temp) +
            "C</font></p>\n" +
            "\t\t\t<p><font size=5 color=\"blue\"><b>Temperature max. - " +
            str(temp_max) + "C</b></font></p>\n" +
            "\t\t\t<p><font size=5 color=\"red\"><b>Temperature min. - " +
            str(temp_min) + "</b></font></p>\n" +
            "\t\t\t<p><font size=5><b>Wind - " + str(wind) +
            "m/s</b></font></p>\n" +
            "\t\t\t<p><font size=5><b>Clouds - " + str(cloud) +
            "%</b></font></p>\n" +
            "\t\t\t<p><font size=5><b>Pressure - " + str(pressure) +
            " hpa</b></font></p>\n" +
            "\t\t\t<p><font size=5><b>Humidity - " + str(humidity) +
            "%</b></font></p>\n" +
            "\t\t\t<hr>\n" +
            "\t\t\tLocation:<br>\n" +
            "\t\t\t<p><font size=5><b>Latitude - 41.62</b></font></p>\n" +
            "\t\t\t<p><font size=5><b>Longitude - 0.62</b></font></p>\n" +
            "\t\t\t<p><font size=5><b>Sky status - " + str(description) +
            "</b></font></p>\n" +
            # Avoid scrolling
            "\t\t\t<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</p>\n" +
            "\t\t\t<hr>\n" +
            "\t\t\t</br>\n" +
            "\t\t\t<font face=arial>Arial</font><br>\n" +
            "\t\t]]></description>\n" +
            "\t\t<Point>\n" +
            "\t\t\t<coordinates>-122.0822035425683,37.42228990140251,0 " +
            "</coordinates>\n" +
            "\t\t</Point>\n" +
            "\t</Placemark>\n" +
            "</kml>")


def random_color():
    r = lambda: random.randint(0, 255)
    return '%02X%02X%02X%02X' % (r(), r(), r(), r())


def create_drone_manage_route(path_name, id_incidence):
    ip_server = get_server_ip()
    url_file = "http://" + str(ip_server)[0:(len(ip_server) - 1)] + \
               ":8000/static/kml/" + "in" + str(id_incidence) + \
               "drone.kml" + "\n"
    manage_kml(path_name + "manage" + str(id_incidence), url_file, 0.5)
    Kml(name="manage" + str(id_incidence) + ".kml",
        url="static/kml/manage" + str(id_incidence) + ".kml").save()
    sync_kml_galaxy()
    time.sleep(2)
