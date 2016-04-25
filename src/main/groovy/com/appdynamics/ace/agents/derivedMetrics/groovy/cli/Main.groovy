package com.appdynamics.ace.agents.derivedMetrics.groovy.cli

import com.appdynamics.ace.com.appdynamics.ace.agents.derivedMetrics.java.CalculationEngine
import com.appdynamics.ace.agents.derivedMetrics.groovy.cli.api.CommandWrapper
import com.appdynamics.ace.com.appdynamics.ace.agents.derivedMetrics.java.MetricValueContainer
import com.appdynamics.ace.util.cli.api.api.CommandlineExecution
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout

/**
 * Created by stefan.marx on 25.09.15.
 */
class Main {



    public static void main(String[] args) {

        CommandlineExecution cle = new CommandlineExecution("DerrivedMetrics");
        cle.setHelpVerboseEnabled(false);

        cle.addCommand(new CommandWrapper("execute","Run one or more calculation scripts",
                [loglevel:[desc: 'set loglevel for debug output (error, info, debug)',opt:true,args:true,def: 'error']],
                {  Map values,scripts ->

                    CalculationEngine engine = new CalculationEngine();

                    ConsoleAppender console = new ConsoleAppender(); //create appender
                    //configure the appender
                    String PATTERN = "%d [%p:%c] %m%n";
                    console.setLayout(new PatternLayout(PATTERN));
                    switch ((values.loglevel as String).toLowerCase()) {
                        case "error" :
                            console.setThreshold(Level.ERROR);
                            break;
                        case "info" :
                            console.setThreshold(Level.INFO);
                            break;
                        case "debug" :
                            console.setThreshold(Level.DEBUG);
                            break;
                        default:
                            console.setThreshold(Level.ERROR);
                    }
                    console.activateOptions();
                    Logger.getRootLogger().addAppender(console);


                    scripts.each(){
                        script ->
                            File f = new File(script);
                            List<MetricValueContainer> results
                            results = engine.execute(f);
                            results.eachWithIndex { MetricValueContainer r,i->
                                println "$i : ${r.valueString}"
                            }


                    }

                    return 0;

                }));



                cle.execute(args);
    }




}