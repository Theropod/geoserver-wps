package org.geoserver.ncl.wps;

import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geoserver.wps.gs.GeoServerProcess;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@DescribeProcess(title = "nclWPS_lnd", description = "Land NCL WPS Example")
public class NclWPSLnd implements GeoServerProcess {

    public String exeCmd(String command, Map<String, String> environment) {
        try {
            /* use processbuilder to easily redirect errorstream to inputstream */
            ProcessBuilder pb =
                    new ProcessBuilder(command.split(" "));
            pb.redirectErrorStream(true);
            Map<String, String> env = pb.environment();
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                env.put(entry.getKey(), entry.getValue());
            }
            Process p = pb.start();
            BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder logString = new StringBuilder();

            String line;
            while ((line = buf.readLine()) != null) {
                System.out.println(line);
                logString.append(line);
                logString.append("\n");
            }
            // wait for 1 minutes
            boolean exitVal = p.waitFor(1, TimeUnit.MINUTES);
            return (exitVal ? "Finished" : "Timeout") + " log: " + logString;
        } catch (Exception e) {
            //TODO: handle exception
            return "failed " + e;
        }
    }

    @DescribeResult(name = "result", description = "output result")
    public String execute(@DescribeParameter(name = "ncl_function", description = "ncl function") String nclFunction,
                          @DescribeParameter(name = "analyse_start_year", description = "analyse start year") String analyseStartYear,
                          @DescribeParameter(name = "analyse_end_year", description = "analyse end year") String analyseEndYear,
                          @DescribeParameter(name = "input_model_file", description = "input model file") String inputModelFile,
                          @DescribeParameter(name = "model_var", description = "Variable in input model data") String modelVar,
                          @DescribeParameter(name = "latN", description = "Region for EOF analysis, lat N") String latN,
                          @DescribeParameter(name = "latS", description = "Region for EOF analysis, lat S") String latS,
                          @DescribeParameter(name = "lonW", description = "Region for EOF analysis, lon W") String lonW,
                          @DescribeParameter(name = "lonE", description = "Region for EOF analysis, lon E") String lonE
    ) {

        // get ncl and data path
        Path currentRelativePath = Paths.get("");
        String workDir = currentRelativePath.toAbsolutePath() + "/data_dir/workspaces/climatemodel/nclWPS_lnd/";
        // result to return
        Map<String, String> result = new HashMap<>();

        // run command, now only pc_model.ncl is included
        try {
            if (nclFunction.equals("pc_model")) {
                // environmental parameters
                Map<String, String> environment = new HashMap<>();

                environment.put("analyse_start_year", analyseStartYear);
                environment.put("analyse_end_year", analyseEndYear);
                environment.put("input_model_file", workDir + inputModelFile);
                environment.put("model_number", "1");
                environment.put("model_name", "CanESM5");
                environment.put("write_plot", "1");
                environment.put("out_plot_type", "png");
                environment.put("write_data", "1");
                environment.put("work_dir", workDir);
                environment.put("out_data_dir", workDir);
                environment.put("out_plot_dir", workDir);
                environment.put("ynrmvmean", "1");
                environment.put("neof", "1");
                environment.put("jopt", "0");
                environment.put("modelvar", modelVar);
                environment.put("latN", latN);
                environment.put("latS", latS);
                environment.put("lonW", lonW);
                environment.put("lonE", lonE);

//                String[] envs = new String[environment.size()];
//                int count = 0;
//                for (Map.Entry<String, String> entry : environment.entrySet()) {
//                    envs[count++] = entry.getKey() + "=" + entry.getValue();
//                }

//               String exportParameters = "export analyse_start_year=" + analyseStartYear + " analyse_end_year=" + analyseEndYear +
//                        " reference_period_start=" + referencePeriodStart + " reference_period_end=" + referencePeriodEnd +
//                        " input_obs_file=" + inputObsFile + " obs_name=" + obsName +
//                        " input_model_file=" + inputModelFile + " model_number=1 model_name=FGOALS_f3_L project=CMIP6 mip=Omon ensemble=r1i1p1f1 grid=gn write_plot=1 out_plot_type=png write_data=1" +
//                        " work_dir=" + workDir + " out_data_dir=" + workDir + " out_plot_dir=" + workDir + " ynsmooth=1" + " smooth_points=" + smoothPoints +
//                        " obsvar=sst modelvar=tos input_model_lat=i input_model_lon=j";

                // run command
                String nclCommand = "ncl " + workDir + nclFunction + ".ncl";
                String nclRunStatus = exeCmd(nclCommand, environment);
                // return serialized result
                result.put("status", nclRunStatus);
                result.put("info", workDir + analyseStartYear + "-" + analyseEndYear + "-annual-PC-CanESM5.png");
                result.put("command", nclCommand);
                return result.toString();
            } else {
                return "ncl function " + nclFunction + " is not included yet";
            }
        } catch (Exception e) {
            //TODO: handle exception
            return e.toString();
        }
    }
}