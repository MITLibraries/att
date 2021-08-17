package edu.mit.att.controllers;

import edu.mit.att.authz.Role;
import edu.mit.att.entity.ApprovedRsasForm;
import edu.mit.att.entity.TransferRequest;
import edu.mit.att.entity.SsaContactsForm;
import edu.mit.att.entity.User;
import edu.mit.att.repository.*;
import edu.mit.att.service.ApprovedRsasFormService;
import edu.mit.att.service.TransferRequestService;
import edu.mit.att.service.UserService;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


@Controller
public class RsaAdmin {
    private final static Logger LOGGER = Logger.getLogger(RsaAdmin.class.getCanonicalName());

    public static final String RSAS_FORMS = "transferRequests";

    @Resource
    private Environment env;

    @Autowired
    ServletContext context;

    @Autowired
    private TransferRequestRepository rsarepo;

    @Autowired
    private RsaFileDataFormRepository filedatarepo;

    @Autowired
    private ApprovedRsasFormRepository approvedrsarepo;

    @Autowired
    private TransferRequestService rsaservice;

    @Autowired
    private UserService userservice;

    @Autowired
    private SsaContactsFormRepository contactrepo;

    @Autowired
    ApprovedRsasFormService approvedrsaservice;

    @Autowired
    UserRepository userrepo;

    // ------------------------------------------------------------------------
    @RequestMapping("/ListDraftRsas")
    public String ListDraftRsas(
            ModelMap model,
            @RequestParam(value = "rsaid", required = false) String rsaid,
            @RequestParam(value = "downloadfailed", required = false) String downloadfailed,
            HttpSession session,
            HttpServletRequest request
    ) {
        LOGGER.log(Level.INFO, "ListDraftRsas");

        // authz logic:

        final String userAttrib = (String) request.getHeader("mail");
        final User user = userrepo.findByEmail(userAttrib).get(0);

        if (!user.getRole().equals(Role.siteadmin.name())) {
            return "Permissions";
        }

        model.addAttribute("rsaid", rsaid);
        model.addAttribute("downloadfailed", downloadfailed);

        List<TransferRequest> transferRequests = rsarepo.findByApprovedFalseAndDeletedFalseOrderByTransferdateAsc();
        LOGGER.info("Found requests:" + transferRequests);

        model.addAttribute(RSAS_FORMS, transferRequests);

        return "ListDraftRsas";
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/ListApprovedRsas")
    public String ListApprovedRsas(
            ModelMap model,
            @RequestParam(value = "rsaid", required = false) String rsaid,
            @RequestParam(value = "downloadfailed", required = false) String downloadfailed,
            HttpSession session, HttpServletRequest request
    ) {
        LOGGER.log(Level.INFO, "ListAppovedRsas");

        // authz logic:

        final String userAttrib = (String) request.getHeader("mail");
        final User user = userrepo.findByEmail(userAttrib).get(0);

        if (!user.getRole().equals(Role.siteadmin.name())) {
            return "Permissions";
        }

        model.addAttribute("rsaid", rsaid);
        model.addAttribute("downloadfailed", downloadfailed);

        List<TransferRequest> transferRequests = rsarepo.findByApprovedTrueAndDeletedFalseOrderByTransferdateAsc();

        LOGGER.log(Level.INFO, "ListAppovedRsas" + transferRequests.toString());


        model.addAttribute(RSAS_FORMS, transferRequests);
        return "ListApprovedRsas";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/EditDraftRsa", method = RequestMethod.GET)
    public String EditDraftRsa(
            ModelMap model,
            @RequestParam(value = "rsaid", required = false) int rsaid,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "EditDraftRsa Get");

        if (rsaid <= 0) {
            return "Home";
        }

        model.addAttribute("defaultaccessrestriction", env.getRequiredProperty("defaults.accessrestriction"));

        final TransferRequest transferRequest = rsarepo.findById(rsaid);

        model.addAttribute("RSAS_FORMS", transferRequest);
        LOGGER.info("File path (get):" + transferRequest.getPath());
        LOGGER.info("Transfer Request:" + transferRequest.toString());
        model.addAttribute("transferRequest", transferRequest);

        model.addAttribute("action", "EditDraftRsa");
        return "EditDraftRsa";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/EditDraftRsa", method = RequestMethod.POST)
    public String EditDraftRsa(
            ModelMap model,
            TransferRequest transferRequest,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "EditDraftRsa Post");

        if (session == null) {
            LOGGER.log(Level.SEVERE, "null session");
            return "Home";
        }

        if (transferRequest == null) {
            LOGGER.log(Level.SEVERE, "transferRequest is null");
            return "Home";
        }

        model.addAttribute("defaultaccessrestriction", env.getRequiredProperty("defaults.accessrestriction"));


        LOGGER.info("Transfer Rquest:" + transferRequest.toString());
        rsaservice.saveForm(transferRequest);

        LOGGER.log(Level.INFO, "All rsas" + rsarepo.findAll());

        String name = (String) session.getAttribute("name");
        String emailaddr = (String) session.getAttribute("email");

//        String useremailaddress = "\"" + name.trim() + "\" <" + emailaddr.trim() + ">";

 /*       EmailSetup emailsetup = new EmailSetup();

        emailsetup.setFrom(env.getRequiredProperty("submit.from"));
        emailsetup.setSubject("Updated Draft Transfer Request");
        emailsetup.setTo(useremailaddress);

        Email email = new Email(emailsetup, sender, velocityEngine, env, context, session, model);
        email.EditDraftRsaSendToUser(transferRequest);*/

        model.addAttribute("name", name);
        model.addAttribute("emailsent", "1");

        model.addAttribute("success", "1");

        if (transferRequest.isApproved()) {
            LOGGER.log(Level.INFO, "draft RSA approved");

            List<SsaContactsForm> contactinfo = transferRequest.getSubmissionAgreement().getSsaContactsForms();

            if (contactinfo != null && contactinfo.size() > 0) {
                LOGGER.log(Level.INFO, "sending approved email");

                String[] emailrecipts = new String[contactinfo.size()];
                String sep = "";
                StringBuilder sb = new StringBuilder();
                int i = 0;
                for (SsaContactsForm contact : contactinfo) {
//                    emailrecipts[i++] = "\"" + contact.getName() + "\" <" + contact.getEmail() + ">";
                    sb.append(sep + "\"" + contact.getName() + "\" <" + contact.getEmail() + ">");
                    sep = ", ";
                }

               /* emailsetup = new EmailSetup();
                emailsetup.setSubject("Draft Transfer Request Approved");
                emailsetup.setToarray(emailrecipts);
                emailsetup.setFrom(env.getRequiredProperty("submit.from"));
                emailsetup.setUsername(session.getAttribute("username").toString());

                email = new Email(emailsetup, sender, velocityEngine, env, context, session, model);
                email.ApprovedDraftRsaSendToContacts(transferRequest);
*/
            }

            model.addAttribute("edited", "1");

            final List<TransferRequest> transferRequests = rsarepo.findByApprovedTrueAndDeletedFalseOrderByTransferdateAsc();
            LOGGER.log(Level.INFO, "Transfer requests (approved):" + transferRequests.toString());
            model.addAttribute("RSAS_FORMS", transferRequests);
            LOGGER.log(Level.INFO, "Returning to list of approved Rsas");
            return "Admin";
        }

        LOGGER.log(Level.INFO, "COntinuing");
        model.addAttribute(RSAS_FORMS, transferRequest);

        model.addAttribute("action", "EditDraftRsa");
        return "EditDraftRsa";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/EditApprovedRsa", method = RequestMethod.GET)
    public String EditApprovedRsa(
            ModelMap model,
            HttpServletRequest request,
            @RequestParam(value = "rsaid", required = false) int rsaid,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "EditAppovedRsas Get");

        if (rsaid <= 0) {
            LOGGER.log(Level.SEVERE, "rsaid <= 0");
            return "Home";
        }

        model.addAttribute("defaultaccessrestriction", env.getRequiredProperty("defaults.accessrestriction"));

        TransferRequest transferRequest = rsarepo.findById(rsaid);
        model.addAttribute("RSAS_FORMS", transferRequest);

        model.addAttribute("action", "EditApprovedRsa");
        return "EditApprovedRsa";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/EditApprovedRsa", method = RequestMethod.POST)
    public String EditApprovedRsa(
            ModelMap model,
            TransferRequest transferRequest,
            HttpServletRequest request,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "EditAppovedRsas Post");

        if (transferRequest == null) {
            LOGGER.log(Level.SEVERE, "transferRequest is null");
            return "Home";
        }

        model.addAttribute("defaultaccessrestriction", env.getRequiredProperty("defaults.accessrestriction"));

        rsaservice.saveForm(transferRequest);

        if (!transferRequest.isApproved()) {
            model.addAttribute("edited", "1");

            List<TransferRequest> transferRequests = rsarepo.findByApprovedFalseAndDeletedFalseOrderByTransferdateAsc();
            model.addAttribute(RSAS_FORMS, transferRequests);

            return "ListDraftRsas";
        }

        model.addAttribute(RSAS_FORMS, transferRequest);

        model.addAttribute("action", "EditApprovedRsa");
        return "EditApprovedRsa";
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/DeleteDraftRsa")
    public String DeleteDraftRsa(
            ModelMap model,
            @RequestParam(value = "rsaid", required = false) int rsaid,
            HttpServletRequest request,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DeleteDraftRsa");

        if (rsaid <= 0) {
            LOGGER.log(Level.SEVERE, "rsaid <= 0");
            return "Home";
        }

        String dropoff = env.getRequiredProperty("dropoff.dir");
        if (dropoff == null || dropoff.equals("")) {
            LOGGER.log(Level.SEVERE, "dropoff is null");
            return "Home";
        }

        try {
            FileUtils.deleteDirectory(new File(String.format(dropoff + "/" + rsaid)));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error: ", ex);
        }

        TransferRequest rsa = rsarepo.findById(rsaid);

        String staffemails = env.getRequiredProperty("org.email");
        String[] emailrecipts = staffemails.split(",");

      /*  EmailSetup emailsetup = new EmailSetup();
        emailsetup.setSubject("Draft Transfer Request deleted: " + rsaid);
        emailsetup.setToarray(emailrecipts);
        emailsetup.setFrom(env.getRequiredProperty("submit.from"));
        emailsetup.setUsername(session.getAttribute("username").toString());

        Email email = new Email(emailsetup, sender, velocityEngine, env, context, session, model);
        email.DeleteDraftRsaSendToStaff(rsa);
        */

        rsa.setDeleted(true);
        rsarepo.save(rsa);

        model.addAttribute("onedeleted", "1");
        model.addAttribute("onedeletedrsaid", rsaid);

        List<TransferRequest> transferRequests = rsarepo.findByApprovedFalseAndDeletedFalseOrderByTransferdateAsc();
        model.addAttribute("RSAS_FORMS", transferRequests);

        return "ListDraftRsas";
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/DeleteApprovedRsa")
    public String DeleteApprovedRsa(
            ModelMap model,
            @RequestParam(value = "rsaid", required = false) int rsaid,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DeleteApprovedRsa");

        if (rsaid <= 0) {
            LOGGER.log(Level.SEVERE, "rsaid <= 0");
            return "Home";
        }

        String dropoff = env.getRequiredProperty("dropoff.dir");
        if (dropoff == null || dropoff.equals("")) {
            LOGGER.log(Level.SEVERE, "dropoff is null");
            return "Home";
        }

        TransferRequest rsa = rsarepo.findById(rsaid);

        approvedrsaservice.recordDeletedRsa(rsa, session);

        rsa.setDeleted(true);
        rsarepo.save(rsa);

        try {
            FileUtils.deleteDirectory(new File(String.format(dropoff + "/" + rsaid)));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error: ", ex);
        }

        model.addAttribute("onedeleted", "1");
        model.addAttribute("onedeletedrsaid", rsaid);

        List<TransferRequest> transferRequests = rsarepo.findByApprovedTrueAndDeletedFalseOrderByTransferdateAsc();
        model.addAttribute(RSAS_FORMS, transferRequests);

        return "ListApprovedRsas";
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/DeleteDraftRsas")
    public String DeleteDraftRsas(
            ModelMap model,
            @RequestParam(value = "rsa", required = false) int[] rsas,
            HttpServletRequest request,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DeleteDraftRsas");

        String dropoff = env.getRequiredProperty("dropoff.dir");
        if (dropoff == null || dropoff.equals("")) {
            LOGGER.log(Level.SEVERE, "dropoff is null");
            List<TransferRequest> transferRequests = rsarepo.findByApprovedFalseAndDeletedFalseOrderByTransferdateAsc();
            model.addAttribute("RSAS_FORMS", transferRequests);
            return "ListDraftRsas";
        }

        if (rsas == null) {
            model.addAttribute("nodeletes", "1");
            List<TransferRequest> transferRequests = rsarepo.findByApprovedFalseAndDeletedFalseOrderByTransferdateAsc();
            model.addAttribute(RSAS_FORMS, transferRequests);
            return "ListDraftRsas";
        }

        List<TransferRequest> deletedrsas = new ArrayList<TransferRequest>();
        for (int rsaid : rsas) {
            if (rsaid > 0) {
                TransferRequest rsa = rsarepo.findById(rsaid);
                deletedrsas.add(rsa);
            }
        }

        int numdeleted = 0;
        String deletersaids = "";
        String sep = "";
        for (int rsaid : rsas) {

            if (rsaid > 0) {

                deletersaids += sep + rsaid;
                sep = ", ";

                model.addAttribute("weredeletes", "1");
                model.addAttribute("deletersaids", deletersaids);

                TransferRequest rsa = rsarepo.findById(rsaid);

                rsa.setDeleted(true);
                rsarepo.save(rsa);

                numdeleted++;

                try {
                    FileUtils.deleteDirectory(new File(dropoff + "/" + rsaid));
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }

        if (numdeleted <= 0) {
            List<TransferRequest> transferRequests = rsarepo.findByApprovedFalseAndDeletedFalseOrderByTransferdateAsc();
            model.addAttribute(RSAS_FORMS, transferRequests);

            return "ListDraftRsas";
        }

        String staffemails = env.getRequiredProperty("org.email");
        String[] emailrecipts = staffemails.split(",");

       /* EmailSetup emailsetup = new EmailSetup();
        emailsetup.setSubject("Draft Transfer Request(s) deleted: " + deletersaids);
        emailsetup.setToarray(emailrecipts);
        emailsetup.setFrom(env.getRequiredProperty("submit.from"));
        emailsetup.setUsername(session.getAttribute("username").toString());

        Email email = new Email(emailsetup, sender, velocityEngine, env, context, session, model);
        email.DeleteDraftRsasSendToStaff(deletedrsas);*/


        List<TransferRequest> transferRequests = rsarepo.findByApprovedFalseAndDeletedFalseOrderByTransferdateAsc();
        model.addAttribute("RSAS_FORMS", transferRequests);

        return "ListDraftRsas";
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/DeleteApprovedRsas")
    public String DeleteApprovedRsas(
            ModelMap model,
            @RequestParam(value = "rsa", required = false) int[] rsaids,
            HttpServletRequest request,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DeleteApprovedRsas");

        String dropoff = env.getRequiredProperty("dropoff.dir");
        if (dropoff == null || dropoff.equals("")) {
            LOGGER.log(Level.SEVERE, "dropoff is null");
            return "Home";
        }

        if (rsaids == null) {
            model.addAttribute("nodeletes", "1");

            final List<TransferRequest> transferRequests = rsarepo.findByApprovedTrueAndDeletedFalseOrderByTransferdateAsc();
            model.addAttribute(RSAS_FORMS, transferRequests);

            return "ListApprovedRsas";
        }

        String deletersaids = "";
        String sep = "";

        for (int rsaid : rsaids) {
            if (rsaid <= 0) {
                LOGGER.log(Level.SEVERE, "rsaid <= 0");
                continue;
            }

            deletersaids += sep + rsaid;
            sep = ", ";

            model.addAttribute("weredeletes", "1");
            model.addAttribute("deletersaids", deletersaids);

            final TransferRequest rsa = rsarepo.findById(rsaid);

            LOGGER.info("About to update Rsas:" + rsa.toString());

            approvedrsaservice.recordDeletedRsa(rsa, session);

            rsa.setDeleted(true);
            try {
                rsarepo.save(rsa);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error saving RSA", e);
            }

            try {
                FileUtils.deleteDirectory(new File(dropoff + "/" + rsaid));
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Error deleting directory", ex);
            }
        }

        final List<TransferRequest> transferRequests = rsarepo.findByApprovedTrueAndDeletedFalseOrderByTransferdateAsc();
        model.addAttribute(RSAS_FORMS, transferRequests);

        return "ListApprovedRsas";
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/ApprovedRsaLog")
    public String ApprovedRsaLog(
            ModelMap model,
            HttpSession session,
            HttpServletRequest request
    ) {
        LOGGER.log(Level.INFO, "ApprovedRsaLog");

        // authz logic:

        final String userAttrib = (String) request.getHeader("mail");
        final User user = userrepo.findByEmail(userAttrib).get(0);

        if (!user.getRole().equals(Role.siteadmin.name())) {
            return "Permissions";
        }



        List<ApprovedRsasForm> rsas = approvedrsarepo.findAllOrderByDatetimeAsc();
        model.addAttribute("data", rsas);

        return "ApprovedRsaLog";
    }
}
