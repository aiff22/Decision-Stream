(defproject decision-stream "1.0"
  :description "This repository provides a basic implementation of the Decision Stream regression and classification algorithms."
  :url "https://github.com/aiff22/Decision-Stream/"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.apache.commons/commons-math3 "3.6.1"]
                 [org.jblas/jblas "1.2.4"]
                 [au.com.bytecode/opencsv "2.4"]]
  :java-source-paths ["src/main/java"]
  :source-paths ["src/main/clojure"]
  :aot :all
  :main DecisionStream
  :uberjar-name "decision-stream.jar"

  ;Following code generates pom.xml for maven project
  :pom-addition [:properties
                 ["maven.compiler.source" "1.8"]
                 ["maven.compiler.target" "1.8"]]
  :pom-plugins [[com.theoryinpractise/clojure-maven-plugin "1.8.1"
                 {:executions ([:execution [:id "compile"]
                                [:goals ([:goal "compile"])]
                                [:phase "compile"]
                                [:configuration
                                 [:namespaces ([:namespace "DecisionStream"])]
                                 [:sourceDirectories ([:sourceDirectory "src/main/clojure"])]
                                 ]])}]
                [org.apache.maven.plugins/maven-assembly-plugin "3.1.0"
                 {:executions ([:execution [:id "package-jar-with-dependencies"]
                                [:goals ([:goal "single"])]
                                [:phase "package"]
                                [:configuration
                                 [:finalName "decision-stream"]
                                 [:appendAssemblyId "false"]
                                 [:outputDirectory "${project.basedir}"]
                                 [:descriptorRefs ([:descriptorRef "jar-with-dependencies"])]
                                 [:archive [:manifest [:mainClass "DecisionStream"]]]
                                 ]])}]]
  )