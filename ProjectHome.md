This is a discrete event simulator developed to evaluate several algorithms ensuring the version consistency of dynamic reconfiguration of component-based distributed systems.

The paper describing the algorithms will appear on [ESEC/FSE 2011](http://2011.esec-fse.org/).


To download and run the simulator, one should:
  1. Be sure JDK 1.6, Eclipse and Subclipse (Eclipse SVN plugin) are properly installed
  1. Start Eclipse and switch to SV NRepositoryExploring perspective
  1. Add a new location with the following URL: http://vcsim.googlecode.com/svn
  1. Check out folder public as a project in the workspace (project name: VCSim2)
  1. Switch to the Java perspective
  1. If needed, include all jar files in directory lib in the buildpath/classpath

One should read the paper in directory doc to understand the aim of the simulation. S/he should also set the options of the JVM as follows: -Xms1000m -Xmx1000m to allocate enough heap memory for the simulator.

Please refer to [ReadMe](http://vcsim.googlecode.com/svn/public/doc/readme.pdf) for more details.