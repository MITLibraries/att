package edu.mit.att.controllers;

//import com.itextpdf.text.Document;
//import com.itextpdf.text.DocumentException;
//import com.itextpdf.text.pdf.PdfWriter;
//import com.itextpdf.tool.xml.XMLWorkerHelper;
import edu.mit.att.entity.*;
import edu.mit.att.repository.OnlineSubmissionRequestFormRepository;
import edu.mit.att.repository.RsaFileDataFormRepository;
import edu.mit.att.repository.TransferRequestRepository;
import edu.mit.att.service.ApprovedRsasFormService;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
//import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class FileOps {
    private final static Logger LOGGER = Logger.getLogger(FileOps.class.getCanonicalName());

    @Resource
    private Environment env;

    @Autowired
    ServletContext context;

    @Autowired
    private TransferRequestRepository rsarepo;

    @Autowired
    private RsaFileDataFormRepository filedatarepo;

    @Autowired
    private OnlineSubmissionRequestFormRepository requestrepo;

    @Autowired
    ApprovedRsasFormService approvedrsaservice;

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/ListApprovedFiles", method = RequestMethod.GET)
    public String ListApprovedFiles(
            ModelMap model,
            HttpServletRequest request,
            @RequestParam("rsaid") int rsaid,
            @RequestParam(value = "downloadfailed", required = false) String downloadfailed,
            HttpServletResponse response,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "ListApprovedFiles Get");

        model.addAttribute("rsaid", rsaid);

        model.addAttribute("downloadfailed", downloadfailed);

        LOGGER.log(Level.INFO, "downloadfailed={0}", new Object[]{downloadfailed});

        TransferRequest rsa = rsarepo.findById(rsaid);
        List<RsaFileDataForm> rsaFileDataForms = rsa.getRsaFileDataForms();
        model.addAttribute("rsaFileDataForms", rsaFileDataForms);

        return "ListApprovedFiles";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/ListDraftFiles", method = RequestMethod.GET)
    public String ListDraftFiles(
            ModelMap model,
            HttpServletRequest request,
            @RequestParam("rsaid") int rsaid,
            @RequestParam(value = "downloadfailed", required = false) String downloadfailed,
            HttpServletResponse response,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "ListDraftFiles Get");

        model.addAttribute("rsaid", rsaid);

        model.addAttribute("downloadfailed", downloadfailed);

        TransferRequest rsa = rsarepo.findById(rsaid);
        List<RsaFileDataForm> rsaFileDataForms = rsa.getRsaFileDataForms();
        model.addAttribute("rsaFileDataForms", rsaFileDataForms);

        return "ListDraftFiles";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/DownloadFile", method = RequestMethod.GET)
    public void DownloadFile(
            ModelMap model,
            HttpServletRequest request,
            @RequestParam("filename") String filename,
            @RequestParam("rsaid") String rsaid,
            HttpServletResponse response,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DownloadFiles Get");

        if (rsaid != null) {
            MyFileUtils fileutils = new MyFileUtils();

            TransferRequest r = rsarepo.findById(Integer.parseInt(rsaid));
            String dropoffdirfull = r.getPath();
            // String dropoffdirfull1 = env.getRequiredProperty("dropoff.dir") + "/" + rsaid;
            fileutils.downloadfile(dropoffdirfull, filename, response, context);
        } else {
            LOGGER.log(Level.SEVERE, "rsaid is null");
        }
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/DownloadZipFile")
    public void DownloadZipFile(
            ModelMap model,
            HttpServletRequest request,
            @RequestParam("rsaid") String rsaid,
            @RequestParam("redirect") String redirect,
            HttpServletResponse response,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DownloadZipFile");

        /*  Utils utils = new Utils();
        if (!utils.setupAdminHandler(model, session, env)) {
            return;
        }*/

        if (rsaid != null) {

            TransferRequest r = rsarepo.findById(Integer.parseInt(rsaid));

            LOGGER.info("Found TransferRequest:" + r);
            LOGGER.info("Found TransferRequest:" + r.getPath());

            MyFileUtils fileutils = new MyFileUtils();
            //String dropoffdirfull = env.getRequiredProperty("dropoff.dir") + "/" + rsaid;
            String dropoffdirfull = r.getPath();

            final File f = new File(dropoffdirfull);
            if (f.exists() == false) { // check if the file was deleted for some external reason
                LOGGER.info("File does not exist:" +  dropoffdirfull); // TODO hide the download button
            } else {

                fileutils.downloadzipfile(model, dropoffdirfull, response, context, redirect, rsaid);
            }
        } else {
            LOGGER.log(Level.SEVERE, "rsaid is null");
        }
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/DeleteApprovedInventory")
    public String DeleteApprovedInventory(
            ModelMap model,
            @RequestParam("rsaid") int rsaid,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DeleteApprovedInventory");

        String dropoff = env.getRequiredProperty("dropoff.dir");
        if (dropoff == null || dropoff.equals("")) {
            LOGGER.log(Level.SEVERE, "dropoff is null");
            return "Home";
        }

        if (rsaid <= 0) {
            LOGGER.log(Level.SEVERE, "rsaid <= 0");
            return "Home";
        }

        try {
            FileUtils.deleteDirectory(new File(String.format(dropoff + "/" + rsaid)));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        TransferRequest rsa = rsarepo.findById(rsaid);
        List<RsaFileDataForm> filedata = rsa.getRsaFileDataForms();
        for (RsaFileDataForm fd : filedata) {
            filedatarepo.delete(fd);
        }
        rsa.setRsaFileDataForms(null);
        rsarepo.save(rsa);

        List<TransferRequest> transferRequests = rsarepo.findByApprovedTrueAndDeletedFalseOrderByTransferdateAsc();
        model.addAttribute("rsasForms", transferRequests);

        return "ListApprovedRsas";
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/DeleteDraftInventory")
    public String DeleteDraftInventory(
            ModelMap model,
            @RequestParam("rsaid") int rsaid,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DeleteDraftInventory");

        String dropoff = env.getRequiredProperty("dropoff.dir");
        if (dropoff == null || dropoff.equals("")) {
            LOGGER.log(Level.SEVERE, "dropoff is null");
            return "Home";
        }

        if (rsaid <= 0) {
            LOGGER.log(Level.SEVERE, "rsaid <= 0");
            return "Home";
        }

        try {
            FileUtils.deleteDirectory(new File(String.format(dropoff + "/" + Integer.toString(rsaid))));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        TransferRequest rsa = rsarepo.findById(rsaid);
        List<RsaFileDataForm> filedata = rsa.getRsaFileDataForms();
        for (RsaFileDataForm fd : filedata) {
            filedatarepo.delete(fd);
        }
        rsa.setRsaFileDataForms(null);
        rsarepo.save(rsa);

        List<TransferRequest> transferRequests = rsarepo.findByApprovedFalseAndDeletedFalseOrderByTransferdateAsc();
        model.addAttribute("rsasForms", transferRequests);

        return "ListDraftRsas";
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/DownloadApprovedRsaCSV")
    public void DownloadApprovedRsaCSV(
            ModelMap model,
            HttpServletResponse response,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DownloadApprovedRsaCSV");

        String fullcsvstring = approvedrsaservice.findAllApprovedTransfersCSV();

        String disposition = "attachment; fileName=ApprovedTransferRequestLog.csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", disposition);
        response.setHeader("content-Length", Integer.toString(fullcsvstring.length()));

        try {
            PrintWriter out = response.getWriter();
            try {
                out.print(fullcsvstring);
            } finally {
                out.close();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    // FIXME: removed. is print pdf functionality necessary?

    // ------------------------------------------------------------------------
   /* @RequestMapping("/PrintPdf")
    public void PrintPdf(
            ModelMap model,
            @RequestParam(value = "ssaid", required = false) int ssaid,
            @RequestParam(value = "ip", required = false) String ip,
            HttpServletResponse response,
            HttpServletRequest request,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "PrintPdf");

        Utils utils = new Utils();
        if (!utils.setupAdminHandler(model, session, env)) {
            return;
        }

        if (ssaid <= 0) {
            LOGGER.log(Level.SEVERE, "PrintPdf Error: ssaid <= 0");
            return;
        }

        List<OnlineSubmissionRequestForm> osrfs = requestrepo.findBySsaid(ssaid);
        if (osrfs == null) {
            LOGGER.log(Level.SEVERE, "online list is null for ssaid={0}", new Object[]{ssaid});
            return;
        }
        if (osrfs.size() != 1) {
            LOGGER.log(Level.SEVERE, "wrong number of online matches={0} for ssaid={1}", new Object[]{osrfs.size(), ssaid});
            return;
        }

        OnlineSubmissionRequestForm osrf = osrfs.get(0);

        if (osrf == null) {
            LOGGER.log(Level.SEVERE, "online is null for ssaid={0}", new Object[]{ssaid});
            return;
        }

        String converteddate = "unknown";
        if (osrf.getDate() != null && !osrf.getDate().equals("")) {
            SimpleDateFormat sdfin = new SimpleDateFormat("yyyy-MM-dd");
            Date d = null;
            try {
                d = sdfin.parse(osrf.getDate());
            } catch (ParseException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            SimpleDateFormat sdfout = new SimpleDateFormat("MMMM dd, yyyy");
            converteddate = sdfout.format(d);
        }

        osrf.setNicedate(converteddate);

        OrgInfo orginfo = new OrgInfo();
        orginfo.setEmail(env.getRequiredProperty("org.email"));
        orginfo.setPhone(env.getRequiredProperty("org.phone"));
        orginfo.setName(env.getRequiredProperty("org.name"));
        orginfo.setNamefull(env.getRequiredProperty("org.namefull"));

        Map<String, Object> vemodel = new HashMap<String, Object>();
        vemodel.put("info", osrf);
        vemodel.put("org", orginfo);

        String htmlstring = VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, "velocity/Request.vm", "UTF8", vemodel);

        boolean wasexception = false;
        String exception = "";
        try {
            Socket socket = new Socket(ip, 9100);
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            InputStream is = new ByteArrayInputStream(htmlstring.getBytes());
            XMLWorkerHelper.getInstance().parseXHtml(writer, document, is);
            document.close();
            out.flush();
            socket.close();
        } catch (DocumentException ex) {
            wasexception = true;
            exception = ex.toString();
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (UnknownHostException ex) {
            wasexception = true;
            exception = ex.toString();
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            wasexception = true;
            exception = ex.toString();
            LOGGER.log(Level.SEVERE, null, ex);
        }

        response.setContentType("text/html;charset=UTF-8");

        PrintWriter responsetoajax = null;
        try {
            responsetoajax = response.getWriter();
        } catch (IOException ex) {
            wasexception = true;
            exception = ex.toString();
            LOGGER.log(Level.SEVERE, null, ex);
        }

        if (wasexception) {
            try {
                responsetoajax.println(exception);
            } finally {
                responsetoajax.close();
            }
        } else {
            try {
                responsetoajax.println("good");
            } finally {
                responsetoajax.close();
            }
        }
    }
*/
    // ------------------------------------------------------------------------
    @RequestMapping("/CreateATImportFile")
    public String CreateATImportFile(
            ModelMap model,
            @RequestParam("rsaid") int rsaid,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "CreateATImportFile");

        if (rsaid <= 0) {
            LOGGER.log(Level.SEVERE, "rsaid <= 0");
            return "Home";
        }

        model.addAttribute("rsaid", rsaid);

        TransferRequest rsa = rsarepo.findById(rsaid);
        model.addAttribute("rsa", rsa);

        return "CreateATImportFile";
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/DownloadATImportFile")
    public void DownloadATImportFile(
            ModelMap model,
            @RequestParam("rsaid") int rsaid,
            @RequestParam(value = "accession", required = false) String accessionnumber,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DownloadATImportFile");

        if (rsaid <= 0) {
            LOGGER.log(Level.SEVERE, "rsaid <= 0");
            return;
        }

        if (accessionnumber == null || accessionnumber.equals("")) {
            LOGGER.log(Level.SEVERE, "accessionnumber null or blank");
            return;
        }

        TransferRequest rsa = rsarepo.findById(rsaid);
        rsa.setAccessionnumber(accessionnumber);
        rsa = rsarepo.save(rsa);

        SubmissionAgreement ssa = rsa.getSubmissionAgreement();

        List<SsaCopyrightsForm> crs = ssa.getSsaCopyrightsForms();

        boolean displaycopyrightelement = true;
        if (crs != null && crs.size() == 1 && crs.get(0) != null && crs.get(0).getCopyright() != null && crs.get(0).getCopyright().equals(env.getRequiredProperty("defaults.copyrightstatement"))) {
            displaycopyrightelement = false;
        }

        List<SsaAccessRestrictionsForm> rs = ssa.getSsaAccessRestrictionsForms();

        boolean displayaccessrestrictionelement = true;
        if (rs != null && rs.size() == 1 && rs.get(0) != null && rs.get(0).getRestriction() != null && rs.get(0).getRestriction().equals(env.getRequiredProperty("defaults.accessrestriction"))) {
            displayaccessrestrictionelement = false;
        }

        String[] accessionparts = rsa.getAccessionnumber().split("\\.");
        String accessionpart1 = rsa.getAccessionnumber();
        String accessionpart2 = "";
        String accessionpart3 = "";
        if (accessionparts.length > 2) {
            accessionpart1 = accessionparts[0];
            accessionpart2 = accessionparts[1];
            accessionpart3 = accessionparts[2];
        }

        Pattern pattern = Pattern.compile("^([^\\.]+)\\.(.*)");
        Matcher matcher = pattern.matcher(ssa.getRecordid());

        String recordidpart1 = ssa.getRecordid();
        String recordidpart2 = "";
        while (matcher.find()) {
            recordidpart1 = matcher.group(1);
            recordidpart2 = matcher.group(2);
        }

        boolean departvalid = true;
        if (rsa.getSubmissionAgreement().getDepartment() == null) {
            departvalid = false;
        }

        Map<String, Object> vemodel = new HashMap<String, Object>();
        vemodel.put("rsa", rsa);

        vemodel.put("recordidpart1", recordidpart1);
        vemodel.put("recordidpart2", recordidpart2);

        vemodel.put("accessionpart1", accessionpart1);
        vemodel.put("accessionpart2", accessionpart2);
        vemodel.put("accessionpart3", accessionpart3);

        vemodel.put("displaycopyrightinfo", "0");
        if (displaycopyrightelement) {
            vemodel.put("displaycopyrightinfo", "1");
        }

        vemodel.put("displayaccessrestrictions", "0");
        if (displayaccessrestrictionelement) {
            vemodel.put("displayaccessrestrictions", "1");
        }

        vemodel.put("departvalid", departvalid);

        //String content = VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, "velocity/ATImportFile.vm", "UTF8", vemodel);

        String content = "test"; //FIXME

        response.setHeader("Content-Disposition", "attachment; filename=\"atimport.xml\"");
        response.setContentType("application/xml");

        try {
            PrintWriter out = response.getWriter();
            try {
                out.print(content);
            } finally {
                out.close();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
}
