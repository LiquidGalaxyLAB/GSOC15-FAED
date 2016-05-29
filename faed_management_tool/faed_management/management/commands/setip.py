import os
from django.core.management.base import BaseCommand, CommandError
import re


def write_ip(ip):
    f = open(os.path.dirname(os.path.dirname(os.path.abspath(__file__))) + '/ipsettings', 'w')
    f.write(ip)
    f.close()


class Command(BaseCommand):
    help = 'Set the <ip> of the galaxy Liquid system.'

    def add_arguments(self, parser):
        parser.add_argument('ip', nargs='?',
                            help='Mandatory galaxy liquid ip address')
        parser.add_argument('addrport', nargs='?',
                            help='Optional port number, or ipaddr:port')

    def handle(self, *args, **options):
        try:
            parsed_ip = options['ip']
            pattern = re.compile("^([m01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                                 "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")
            if pattern.match(parsed_ip):
                write_ip(parsed_ip);
                self.stdout.write(self.style.SUCCESS('Successfully changed the ip to "%s"' % parsed_ip))
                os.system("mkdir /tmp/kml")
                os.system("touch /tmp/kml/kmls.txt")
                if not options['addrport']:
                    os.system("python manage.py runserver")
                else:
                    write_ip(parsed_ip);
                    addrport = options['addrport']
                    os.system("python manage.py runserver " + addrport)
            else:
                self.stdout.write(self.style.SUCCESS('Ip "%s" inserted incorrect format' % parsed_ip))
        except:
            raise CommandError('Galaxy liquid ip must be set')
