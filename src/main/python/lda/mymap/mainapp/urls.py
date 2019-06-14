from django.conf.urls import url

from . import views

urlpatterns = [
    url(r'^$', views.index, name='index'),
    url(r'^ajax_lines/$', views.ajax_lines, name='ajax_lines'),
]
