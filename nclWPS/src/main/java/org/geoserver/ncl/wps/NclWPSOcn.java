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

@DescribeProcess(title = "nclWPS_ocn", description = "Ocean NCL WPS Example")
public class NclWPSOcn implements GeoServerProcess {

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
                          @DescribeParameter(name = "reference_period_start", description = "reference period start") String referencePeriodStart,
                          @DescribeParameter(name = "reference_period_end", description = "reference period end") String referencePeriodEnd,
                          @DescribeParameter(name = "input_obs_file", description = "input obs file") String inputObsFile,
                          @DescribeParameter(name = "obs_name", description = "obs name") String obsName,
                          @DescribeParameter(name = "smooth_points", description = "smooth points") String smoothPoints
    ) {

        // get ncl and data path
        Path currentRelativePath = Paths.get("");
        String workDir = currentRelativePath.toAbsolutePath() + "/data_dir/workspaces/climatemodel/nclWPS_ocn/";
        // result to return
        Map<String, String> result = new HashMap<>();

        // run command, now only amo_mothly.ncl is included
        try {
            if (nclFunction.equals("amo_monthly")) {
                // environmental parameters
                Map<String, String> environment = new HashMap<>();

                environment.put("analyse_start_year", analyseStartYear);
                environment.put("analyse_end_year", analyseEndYear);
                environment.put("reference_period_start", referencePeriodStart);
                environment.put("reference_period_end", referencePeriodEnd);
                environment.put("input_obs_file", workDir + inputObsFile);
                environment.put("obs_name", obsName);
                environment.put("input_model_file", workDir + inputModelFile);
                environment.put("model_number", "1");
                environment.put("model_name", "FGOALS_f3_L");
                environment.put("project", "CMIP6");
                environment.put("mip", "Omon");
                environment.put("ensemble", "r1i1p1f1");
                environment.put("grid", "gn");
                environment.put("write_plot", "1");
                environment.put("out_plot_type", "png");
                environment.put("write_data", "1");
                environment.put("work_dir", workDir);
                environment.put("out_data_dir", workDir);
                environment.put("out_plot_dir", workDir);
                environment.put("ynsmooth", "1");
                environment.put("smooth_points", smoothPoints);
                environment.put("obsvar", "sst");
                environment.put("modelvar", "tos");
                environment.put("input_model_lat", "i");
                environment.put("input_model_lon", "j");

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
                result.put("info", workDir + analyseStartYear + "-" + analyseEndYear + "AMO-smoothedwith-" + smoothPoints + "points.png");
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