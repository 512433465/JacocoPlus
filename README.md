JaCoCo Java Code Coverage Library
=================================

[![Build Status](https://travis-ci.org/jacoco/jacoco.svg?branch=master)](https://travis-ci.org/jacoco/jacoco)
[![Build status](https://ci.appveyor.com/api/projects/status/g28egytv4tb898d7/branch/master?svg=true)](https://ci.appveyor.com/project/JaCoCo/jacoco/branch/master)
[![Maven Central](https://img.shields.io/maven-central/v/org.jacoco/jacoco.svg)](http://search.maven.org/#search|ga|1|g%3Aorg.jacoco)

JaCoCo is a free Java code coverage library distributed under the Eclipse Public
License. Check the [project homepage](http://www.jacoco.org/jacoco)
for downloads, documentation and feedback.

Please use our [mailing list](https://groups.google.com/forum/?fromgroups=#!forum/jacoco)
for questions regarding JaCoCo which are not already covered by the
[extensive documentation](http://www.jacoco.org/jacoco/trunk/doc/).

Note: 欢迎一起开发，有问题提issue
-------------------------------------------------------------------------

JaCoCo二次开发基于Git分支差分实现增量代码覆盖率

原理：通过使用org.eclipse.jgit比较新旧分支代码差分，取得变更行信息，生成报告时高亮线上变更行信息，未检出变更行不做处理。
      从而达到，增量显示代码覆盖率的目的。当然当不传代码分支时默认展示全量覆盖率。

使用方法：一，Jacoco包的使用，具体配置百度Jacoco的Tomcat配置
         1，懒伸手党模式：使用我二次开发后上传的[Release包](https://github.com/512433465/JacocoPlus/releases)代替自己下载的Jacoco包。
         2，自己动手模式：下载代码，执行mvn clean package -Dmaven.javadoc.test=true -Dmaven.test.skip=true自行打包。注意事项IDE下载后，在IDE中执行maven打包命令可能不成功。
         建议在命令行中执行maven打包命令。打包成功后，用官网包lib/jacocoagent.jar 替换自己新打包生成的jacocoagent.jar文件，否则可能引起Tomcat无法启动。