# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
import datetime
from django.utils.timezone import utc


class Migration(migrations.Migration):

    dependencies = [
        ('kmls_management', '0001_initial'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='kml',
            name='href',
        ),
        migrations.AddField(
            model_name='kml',
            name='url',
            field=models.CharField(default=datetime.datetime(2015, 7, 13, 10, 43, 15, 785980, tzinfo=utc), max_length=100),
            preserve_default=False,
        ),
    ]
