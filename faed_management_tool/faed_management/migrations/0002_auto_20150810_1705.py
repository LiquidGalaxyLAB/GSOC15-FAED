# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('faed_management', '0001_initial'),
    ]

    operations = [
        migrations.AlterField(
            model_name='meteostation',
            name='humidity',
            field=models.FloatField(default=0.0),
        ),
        migrations.AlterField(
            model_name='meteostation',
            name='precipitation',
            field=models.FloatField(default=0.0),
        ),
        migrations.AlterField(
            model_name='meteostation',
            name='pressure',
            field=models.FloatField(default=0.0),
        ),
        migrations.AlterField(
            model_name='meteostation',
            name='tmp_max',
            field=models.FloatField(default=0.0),
        ),
        migrations.AlterField(
            model_name='meteostation',
            name='tmp_min',
            field=models.FloatField(default=0.0),
        ),
        migrations.AlterField(
            model_name='meteostation',
            name='tmp_now',
            field=models.FloatField(default=0.0),
        ),
        migrations.AlterField(
            model_name='meteostation',
            name='wind',
            field=models.FloatField(default=0.0),
        ),
    ]
