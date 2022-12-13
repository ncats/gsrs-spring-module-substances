#!/usr/bin/env perl

use strict;
use warnings;
use XML::MyXML qw(:all);
use Test::More;
use JSON;
use Path::Tiny;

# Enter the current gsrs deployment version
my $gdv = '3.0.3';
my $gdvsnapshot = $gdv.'-SNAPSHOT';

# These checks are peformed before deployment 
# To make sure pom version information is correct
# and we have some needed files

print "^^^ Checking gsrs-module-spring-substances ^^^\n\n";
print "The script is assuming the version $gdvsnapshot\n";
print "Edit file if needed.\n\n";


my $installExtraJars_script_text = path('./installExtraJars.sh')->slurp_utf8();

my $root_obj = xml_to_object('.' . '/pom.xml', {file=>1});    
print "=== root ===\n";
ok($root_obj ->path('/project/properties/gsrs.version')->text eq $gdvsnapshot, 'gsrs.version'); 
ok($root_obj ->path('/project/properties/gsrs.substance.version')->text eq $gdvsnapshot, 'gsrs.substance.version'); 
print "\n";

my @modules = $root_obj->path('/project/modules/');
for my $module (@modules) {
  my $project = $module->text;  
  if ($project eq 'gsrs-discovery') { 
    my $obj = xml_to_object($project . '/pom.xml', {file=>1}); 
    print "=== $project ===\n";
    ok($obj->path('/project/version')->text eq $gdvsnapshot, '(discovery) -- version'); 
    print "\n";
  } else { 
    my $obj = xml_to_object($project . '/pom.xml', {file=>1}); 
    print "=== $project ===\n";
    ok($obj->path('/project/parent/version')->text eq $gdvsnapshot, 'parent -- version'); 
    print "\n";

    if ($project eq 'gsrs-fda-substance-extension') {
      for my $dep ($obj->path('/project/dependencies/')) {
        if ($dep->path('groupId')->text eq 'gov.nih.ncats' and $dep->path('artifactId')->text eq 'applications-api') {
          my $jar_path = 'extraJars/'.$dep->path('artifactId')->text . '-' . $dep->path('version')->text .'.jar';
          test_extra_jar_exists($jar_path);
          test_extra_jar_in_install_script($jar_path);
        }
        if ($dep->path('groupId')->text eq 'gov.nih.ncats' and $dep->path('artifactId')->text eq 'clinical-trials-api') {
          my $jar_path = 'extraJars/'.$dep->path('artifactId')->text . '-' . $dep->path('version')->text .'.jar';
          test_extra_jar_exists($jar_path);
          test_extra_jar_in_install_script($jar_path);
        }
        if ($dep->path('groupId')->text eq 'gov.nih.ncats' and $dep->path('artifactId')->text eq 'products-api') {
          my $jar_path = 'extraJars/'.$dep->path('artifactId')->text . '-' . $dep->path('version')->text .'.jar';
          test_extra_jar_exists($jar_path);
          test_extra_jar_in_install_script($jar_path);
        }
      }
      print "\n";
    }
    
    if ($project eq 'gsrs-module-substances-core') {
      for my $dep ($obj->path('/project/dependencies/')) {
        if ($dep->path('groupId')->text eq 'gov.nih.ncats' and $dep->path('artifactId')->text eq 'molwitch-renderer') {
          my $jar_path = 'extraJars/'.$dep->path('artifactId')->text . '-' . $dep->path('version')->text .'.jar';
          test_extra_jar_exists($jar_path);
          test_extra_jar_in_install_script($jar_path);
        }
      }
      print "\n";
    }    
  }
  
  
  
  sub test_extra_jar_in_install_script { 
    my $path = shift;
    die "jar $path must be defined\n" if (!$path);    
    ok($installExtraJars_script_text =~ m/\Q$path/, 'install script contains: ' . $path);
  }        


  sub test_extra_jar_exists { 
    my $path = shift;
    die "jar $path must be defined\n" if (!$path);
    ok (-f $path, 'path exists: '. $path);
    return;                     
  } 

  
}

# Questions: 
   # why are cdk and structure indexer part of starter and not substances? 
   # https://cdk.github.io/
   # how to check cdk files ? 
   # cdk is currently at version 2.8, should we be using 2.6.

done_testing();


__END__


