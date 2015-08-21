# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='City',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('name', models.CharField(max_length=100)),
                ('lat', models.FloatField(default=b'0')),
                ('lng', models.FloatField(default=b'0')),
            ],
        ),
        migrations.CreateModel(
            name='Drone',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('name', models.CharField(max_length=50)),
                ('plate', models.CharField(max_length=50)),
                ('origin_lat', models.FloatField(null=True)),
                ('origin_lon', models.FloatField(null=True)),
                ('destination_lat', models.FloatField(null=True)),
                ('destination_lon', models.FloatField(null=True)),
                ('emergency', models.CharField(max_length=50, null=True)),
                ('battery_life', models.PositiveSmallIntegerField(default=100)),
            ],
        ),
        migrations.CreateModel(
            name='DropPoint',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('name', models.CharField(max_length=50)),
                ('description', models.TextField()),
                ('latitude', models.FloatField()),
                ('longitude', models.FloatField()),
                ('altitude', models.FloatField()),
                ('is_available', models.BooleanField(default=True)),
                ('city', models.ForeignKey(to='faed_management.City')),
            ],
        ),
        migrations.CreateModel(
            name='Hangar',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('name', models.CharField(max_length=50)),
                ('description', models.TextField()),
                ('latitude', models.FloatField()),
                ('longitude', models.FloatField()),
                ('altitude', models.FloatField()),
                ('radius', models.FloatField()),
                ('is_available', models.BooleanField(default=True)),
                ('city', models.ForeignKey(to='faed_management.City')),
                ('drone', models.ForeignKey(to='faed_management.Drone')),
                ('drop_points', models.ManyToManyField(to='faed_management.DropPoint')),
            ],
        ),
        migrations.CreateModel(
            name='MeteoStation',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('name', models.CharField(max_length=50)),
                ('description', models.TextField()),
                ('latitude', models.FloatField()),
                ('longitude', models.FloatField()),
                ('altitude', models.FloatField()),
                ('is_available', models.BooleanField(default=True)),
                ('tmp_now', models.FloatField()),
                ('tmp_max', models.FloatField()),
                ('tmp_min', models.FloatField()),
                ('humidity', models.FloatField()),
                ('precipitation', models.FloatField()),
                ('pressure', models.FloatField()),
                ('wind', models.FloatField()),
                ('city', models.ForeignKey(to='faed_management.City')),
            ],
        ),
        migrations.CreateModel(
            name='StyleURL',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('name', models.CharField(max_length=50)),
                ('href', models.URLField()),
                ('scale', models.FloatField()),
            ],
        ),
        migrations.AddField(
            model_name='meteostation',
            name='style_url',
            field=models.ForeignKey(to='faed_management.StyleURL'),
        ),
        migrations.AddField(
            model_name='hangar',
            name='style_url',
            field=models.ForeignKey(to='faed_management.StyleURL'),
        ),
        migrations.AddField(
            model_name='droppoint',
            name='style_url',
            field=models.ForeignKey(to='faed_management.StyleURL'),
        ),
        migrations.AddField(
            model_name='drone',
            name='style_url',
            field=models.ForeignKey(to='faed_management.StyleURL'),
        ),
    ]
