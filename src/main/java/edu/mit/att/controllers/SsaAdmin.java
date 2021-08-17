package edu.mit.att.controllers;

//import org.apache.commons.io.FileUtils;
//import org.apache.velocity.app.VelocityEngine;

import edu.mit.att.authz.Role;
import edu.mit.att.entity.*;
import edu.mit.att.repository.*;
import edu.mit.att.service.DepartmentService;
import edu.mit.att.service.SsasFormService;
import edu.mit.att.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
//import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
//import org.springframework.ui.velocity.VelocityEngineUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Controller
public class SsaAdmin {
    private final static Logger LOGGER = Logger.getLogger(SsaAdmin.class.getCanonicalName());
    public static final String SSAS_FORM = "submissionAgreement";

    @Resource
    private Environment env;

    @Autowired
    ServletContext context;

    /*private static VelocityEngine velocityEngine;

    @Autowired
    public void setVelocityEngine(VelocityEngine ve) {
        velocityEngine = ve;
    }*/

    /*private JavaMailSenderImpl sender;

    @Autowired
    public void setSender(JavaMailSenderImpl sender) {
        this.sender = sender;
    }
*/
    @Autowired
    private DepartmentRepository departmentrepo;

    @Autowired
    private SubmissionAgreementRepository ssarepo;

    @Autowired
    private TransferRequestRepository rsarepo;

    @Autowired
    private RsaFileDataFormRepository rsaFileDataFormRepository;

    @Autowired
    private SsaContactsFormRepository contactrepo;

    @Autowired
    private SsaAccessRestrictionsFormRepository accessrestrictionrepo;

    @Autowired
    private SsaCopyrightsFormRepository copyrightrepo;

    @Autowired
    private SsaFormatTypesFormRepository formattyperepo;

    @Autowired
    private OnlineSubmissionRequestFormRepository requestrepo;

    @Autowired
    private SsasFormService ssaservice;

    @Autowired
    private DepartmentService departmentservice;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userrepo;

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/ListSsas", method = RequestMethod.GET)
    public String ListSsas(ModelMap model, HttpSession session, HttpServletRequest request) {
        LOGGER.log(Level.INFO, "ListSsas Get");



        model.addAttribute("alldeleted", "0");
        model.addAttribute("somenotdeleted", "0");
        model.addAttribute("newssa", "1");

        //List<SubmissionAgreement> ssas = ssarepo.findAll(); // TODO

        List<Department> departments = Collections.emptyList();

        // authz logic:

        final String userAttrib = (String) request.getHeader("mail");
        final User user = userrepo.findByEmail(userAttrib).get(0);

        LOGGER.log(Level.INFO, "User role:" + user.getRole());


        if (user.getRole().equals(Role.deptadmin.name())) {
            final Set<Department> userDepartments = user.getDepartments();
            departments = new ArrayList<>(userDepartments);
        } else if (user.getRole().equals(Role.siteadmin.name())){
            departments = departmentrepo.findAllOrderByNameAsc();
        } else {
            return "Permissions";
        }

        LOGGER.log(Level.INFO, "Found departments:" + departments);


        //List<SubmissionAgreement> ssas = ssarepo.findByDeletedFalseOrderByCreationdateAsc();

        List<SubmissionAgreement> ssas = new ArrayList<>();

        // Bug - doesn't work for edited SSAs, somehow they are missed.

        for (final Department d : departments) {
            List<SubmissionAgreement> submissionAgreements = ssarepo.findAllForDepartmentId(d.getId()); //TODO does this include deleted ones?
            ssas.addAll(submissionAgreements);
            LOGGER.log(Level.INFO, "SSAs list:" + ssas);
        }

        LOGGER.info("Found SSAs:" + ssas.toString());

        model.addAttribute(SSAS_FORM, ssas);

        return "ListSsas";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/CreateSsa", method = RequestMethod.GET)
    public String CreateSsa(
            ModelMap model,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "CreateSsa Get");

        model.addAttribute("newssa", "1");

        String todayssqldate = String.format("%1$tY-%1$tm-%1$td", Calendar.getInstance());

        List<SsaCopyrightsForm> newcrfs = new ArrayList<SsaCopyrightsForm>();
        SsaCopyrightsForm crf = new SsaCopyrightsForm();
        crf.setCopyright(env.getRequiredProperty("defaults.copyrightstatement"));
        newcrfs.add(crf);

        SubmissionAgreement submissionAgreement = new SubmissionAgreement();

        submissionAgreement.setRecordstitle(env.getRequiredProperty("defaults.recordstitle"));
        submissionAgreement.setSsaCopyrightsForms(newcrfs);
        submissionAgreement.setRetentionschedule(env.getRequiredProperty("defaults.retentionschedule"));
        submissionAgreement.setDescriptionstandards(env.getRequiredProperty("defaults.archivedescriptionstandards"));
        submissionAgreement.setCreationdate(todayssqldate);

        // access restrictions... None... not an entry

        final List<Department> dfs = departmentrepo.findAllOrderByNameAsc();

        final List<Department> departmentsForDropDown = new ArrayList<>();

        // Solution for EditSSA problem where multiple submission agreements get associated with the same department

        for (final Department d : dfs) {
            final List<SubmissionAgreement> ss = ssarepo.findAllForDepartmentId(d.getId());
            if (ss.isEmpty() == true) {
                departmentsForDropDown.add(d);
            } else {
                LOGGER.log(Level.INFO, "Filtered out, as it is already associated with a submission agreement:{}",
                        d.getId());
            }
        }

        submissionAgreement.setDropdownDepartments(departmentsForDropDown);

        model.addAttribute("defaultaccessrestriction", env.getRequiredProperty("defaults.accessrestriction"));

        model.addAttribute(SSAS_FORM, submissionAgreement);

        model.addAttribute("action", "CreateSsa");
        return "CreateSsa";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/CreateSsa", method = RequestMethod.POST)
    public ModelAndView CreateSsa(
            final SubmissionAgreement submissionAgreement,
            BindingResult result,
            @RequestParam(value = "departmentid", required = false) Department selectedDepartment,
            final RedirectAttributes redirectAttributes,
            ModelMap model,
            HttpServletRequest request,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "CreateSsa Post");

        LOGGER.log(Level.INFO, "SSA for department:" + selectedDepartment);

        if (result.hasErrors()) { //osm:?
            LOGGER.log(Level.SEVERE, "createSsaPost: has errors");
            model.addAttribute("action", "CreateSsa");
            return new ModelAndView("/CreateSsa");
        }

        model.addAttribute("defaultaccessrestriction", env.getRequiredProperty("defaults.accessrestriction"));

        if (submissionAgreement == null) {
            LOGGER.log(Level.INFO, "Null SSAForm object");
        }

        //session.setAttribute("name", "osman"); //FIXME. change to user logged in

        LOGGER.log(Level.INFO, "Saving (pre1):" + selectedDepartment);

        //submissionAgreement.setSsaCopyrightsForms(Collections.emptyList());

        LOGGER.log(Level.INFO, "Copyrights form:" + submissionAgreement.getSsaCopyrightsForms());

        submissionAgreement.setSsaCopyrightsForms(submissionAgreement.getSsaCopyrightsForms());

        LOGGER.log(Level.INFO, "Saving (pre2):" + selectedDepartment);


        final Department d = departmentrepo.findById(selectedDepartment.getId());

        // This is to prevent association of a submission agreement with multiple departments. This causes an error
        // when editing a submission agreement.

        if (ssarepo.findAllForDepartmentId(d.getId()).size() > 0) {
            LOGGER.info("Warning - A submission agreement is already associated with this department:"
                    + ssarepo.findAll().toString());
            //redirectAttributes.addFlashAttribute("message", "Warning - A submission agreement is already associated with this department");
            //final ModelAndView mav = new ModelAndView();
            //mav.setViewName("ListSsas");
            //return mav;
        }



        ssaservice.create(submissionAgreement, d, session, request);

        LOGGER.info("Saved object:" + ssarepo.findAll().toString());

        // TODO: workaround for user multiple department problem:


        List<User> u = userService.findAll();

        for (User user : u) {
            LOGGER.info("User departments (post):" + user.getDepartments().toString());
        }

        final ModelAndView mav = new ModelAndView();
        //String message = "New SSA " + submissionAgreement.getRecordid() + " was successfully created.";

        final List<SubmissionAgreement> ssas = ssarepo.findByDeletedFalseOrderByCreationdateAsc();
        //final List<SubmissionAgreement> ssas = ssarepo.findAll();
        model.addAttribute(SSAS_FORM, ssas);;


        List<SubmissionAgreement> ssasFormsUser = ssarepo.findAllSsasForUsername(u.get(0).getUsername());

        LOGGER.info("All SSA for user:" + ssasFormsUser.toString());


        LOGGER.log(Level.INFO, "Now returning to the main page");

        mav.setViewName("ListSsas");

        //redirectAttributes.addFlashAttribute("message", message);

        return mav;
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/SsaDeleteWarning")
    public String SsaDeleteWarning(
            ModelMap model,
            @RequestParam("ssaid") int ssaid,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "SsaDeleteWarning");

       /* Utils utils = new Utils();
        if (!utils.setupAdminHandler(model, session, env)) {
            return "Home";
        }*/

        SubmissionAgreement ssa = ssarepo.findById(ssaid);
        model.addAttribute("ssa", ssa);

        return "SsaDeleteWarning";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/DeleteSsa", method = RequestMethod.GET)
    public String DeleteSsa(
            ModelMap model,
            @RequestParam("ssaid") int ssaid,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DeleteSsa Get");

        /*Utils utils = new Utils();
        if (!utils.setupAdminHandler(model, session, env)) {
            return "Home";
        }*/

        if (ssaid == 0) {
            LOGGER.log(Level.INFO, "Cannot delete -- ssaid = 0");
            return "Home";
        }

        SubmissionAgreement ssaform = ssarepo.findById(ssaid);
        if (ssaform != null) {
            ssaform.setDeleted(true);
            //ssarepo.save(ssaform);
            ssarepo.delete(ssaform);
        }

        model.addAttribute("alldeleted", "0");
        model.addAttribute("somenotdeleted", "0");

        List<SubmissionAgreement> ssas = ssarepo.findByDeletedFalseOrderByCreationdateAsc();
        model.addAttribute(SSAS_FORM, ssas);

        return "ListSsas";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/DeleteSsas", method = RequestMethod.POST)
    public String DeleteSsas(
            ModelMap model,
            @RequestParam("ssa") int[] ssaids,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DeleteSsas Post");

     /*   Utils utils = new Utils();
        if (!utils.setupAdminHandler(model, session, env)) {
            return "Home";
        }*/

        model.addAttribute("alldeleted", "0");
        model.addAttribute("somenotdeleted", "0");

        if (ssaids == null || ssaids.length == 0) {
            model.addAttribute("nodeletes", "1");

            List<SubmissionAgreement> ssas = ssarepo.findByDeletedFalseOrderByCreationdateAsc();
            model.addAttribute(SSAS_FORM, ssas);
            return "ListSsas";
        }

        StringBuilder sb = new StringBuilder();
        String sep = "";
        int rejectedcnt = 0;
        for (int ssaid : ssaids) {

            List<TransferRequest> rsas = rsarepo.findAllForSsaOrderByTransferdateAsc(ssaid);
            if (rsas == null || rsas.size() == 0) {
                SubmissionAgreement ssa = ssarepo.findById(ssaid);
                ssa.setDeleted(true);
                ssarepo.save(ssa);
                //ssarepo.delete(ssa);

		/*
        List<SsaContactsForm> cis = contactrepo.findAllBySsaIdOrderByNameAsc(ssaid);
		if(cis != null){
		    for(SsaContactsForm ci : cis){
			contactrepo.delete(ci);
		    }
		}
		*/
        /*
		List<SsaAccessRestrictionsForm> ars = accessrestrictionrepo.findAllBySsaIdOrderByRestrictionAsc(ssaid);
		if(ars != null){
		    for(SsaAccessRestrictionsForm ar : ars){
			accessrestrictionrepo.delete(ar);
		    }
		}
		
		List<SsaFormatTypesForm> fts = formattyperepo.findAllBySsaIdOrderByFormattypeAsc(ssaid);
		if(fts != null){
		    for(SsaFormatTypesForm ft : fts){
			formattyperepo.delete(ft);
		    }
		}
		*/
            } else {
                sb.append(sep + Integer.toString(ssaid));
                sep = ", ";
                rejectedcnt++;
            }

        }

        String rejectedssas = sb.toString();
        if (rejectedcnt == 0) {
            model.addAttribute("alldeleted", "1");
        } else {
            model.addAttribute("somenotdeleted", "1");
            model.addAttribute("rejectedssas", rejectedssas);
        }

        List<SubmissionAgreement> ssas = ssarepo.findByDeletedFalseOrderByCreationdateAsc();
        model.addAttribute(SSAS_FORM, ssas);

        return "ListSsas";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/EditSsa", method = RequestMethod.GET)
    public String EditSsa(ModelMap model, @RequestParam("id") int ssaid, HttpServletRequest request, HttpSession session) {
        LOGGER.log(Level.INFO, "EditSsa Get for:" + ssaid);

        /*Utils utils = new Utils();
        if (!utils.setupAdminHandler(model, session, env)) {
            return "Home";
        }*/

        model.addAttribute("newssa", "0");

        SubmissionAgreement submissionAgreement = ssarepo.findById(ssaid);

        LOGGER.log(Level.INFO, "All objects:" + ssarepo.findAll());

        if (env.getRequiredProperty("defaults.accessrestriction") != null) {
            model.addAttribute("defaultaccessrestriction", env.getRequiredProperty("defaults.accessrestriction"));
        }

        List<Department> df = departmentservice.findAllNotAssociatedWithOtherSsaOrderByName(ssaid);
        //TODO: this is what causes theproblem.
        // if an SSA is already associated with a department, it throws things off.
        // Each department has a unique submission agreement.

        LOGGER.log(Level.INFO, "Departments for the submission agreement:" + df);


        submissionAgreement.setDropdownDepartments(df);
        model.addAttribute(SSAS_FORM, submissionAgreement);

        model.addAttribute("dropdownDepartments", df);

        model.addAttribute("action", "EditSsa");
        return "EditSsa";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/EditSsa", method = RequestMethod.POST)
    public String EditSsa(
            ModelMap model,
            SubmissionAgreement submissionAgreement,
            @RequestParam("id") int ssaid,
            @RequestParam(value = "departmentid", required = false) Department selectedDepartment,
            HttpServletRequest request,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "EditSsa Post:{}", ssaid);

        if (submissionAgreement == null) {
            model.addAttribute("submssionAgreement", submissionAgreement);
            return "EditSsa";
        }

        model.addAttribute("newssa", "0");
        model.addAttribute("failed", "0");
        model.addAttribute("emailrecips", "");
        model.addAttribute("invalidaddresses", "0");

        if (env.getRequiredProperty("defaults.accessrestriction") != null) {
            model.addAttribute("defaultaccessrestriction", env.getRequiredProperty("defaults.accessrestriction"));
        }

        ssaservice.saveForm(submissionAgreement, selectedDepartment);

        List<Department> df = departmentservice.findAllNotAssociatedWithOtherSsaOrderByName(ssaid);
        submissionAgreement.setDropdownDepartments(df);

        model.addAttribute(SSAS_FORM, submissionAgreement);

        /*if (submissionAgreement.isApproved()) {
            List<SsaContactsForm> contactinfo = submissionAgreement.getSsaContactsForms();

            if (contactinfo != null && contactinfo.size() > 0) {
                LOGGER.log(Level.INFO, "sending approved email");

                int size = 0;
                for (SsaContactsForm contact : contactinfo) {
                    if (!contact.getEmail().matches("^\\s*$")) {
                        size++;
                    }
                }

                String[] emailrecipts = new String[size];
                String sep = "";
                StringBuilder sb = new StringBuilder();
                int numrecipts = 0;
                for (SsaContactsForm contact : contactinfo) {
                    if (!contact.getEmail().matches("^\\s*$")) {
                        emailrecipts[numrecipts++] = "\"" + contact.getName() + "\" <" + contact.getEmail() + ">";
                        sb.append(sep + "\"" + contact.getName() + "\" <" + contact.getEmail() + ">");
                        sep = ", ";
                    }
                }

                if (numrecipts > 0) {

                    EmailSetup emailsetup = new EmailSetup();
                    emailsetup.setSubject("Submission Agreement approved: " + submissionAgreement.getId());
                    emailsetup.setToarray(emailrecipts);
                    emailsetup.setFrom(env.getRequiredProperty("submit.from"));
                    emailsetup.setUsername(session.getAttribute("username").toString());

                    Email email = new Email(emailsetup, sender, velocityEngine, env, context, session, model);
                    email.EditSsaApprovedSendToCreators(submissionAgreement);

                }
            }
        }*/

        model.addAttribute("action", "EditSsa");
        return "Home";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/DeleteContact", method = RequestMethod.GET)
    public String DeleteContact(
            @RequestParam(value = "id", required = false) int id,
            @RequestParam(value = "ssaid", required = false) int ssaid,
            ModelMap model,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DeleteContact Get");

        if (id == 0) {
            LOGGER.log(Level.SEVERE, "DeleteContact Get: contactid={0}", new Object[]{id});
            model.addAttribute("action", "EditSsa");
            return "EditSsa";
        }
        if (ssaid == 0) {
            LOGGER.log(Level.SEVERE, "DeleteContact Get: ssaid={0}", new Object[]{ssaid});
            model.addAttribute("action", "EditSsa");
            return "EditSsa";
        }

        SubmissionAgreement submissionAgreement = ssarepo.findById(ssaid);

        List<SsaContactsForm> cs = submissionAgreement.getSsaContactsForms();
        List<SsaContactsForm> newcs = new ArrayList<SsaContactsForm>();
        for (SsaContactsForm cf : cs) {
            if (cf.getId() != id) {
                newcs.add(cf);
            }
        }
        submissionAgreement.setSsaContactsForms(newcs);

        SsaContactsForm con = contactrepo.findById(id);
        LOGGER.log(Level.INFO, "delete ssaContactsForms id={0} name={1}", new Object[]{con.getId(), con.getName()});
        contactrepo.delete(con);

        List<Department> df = departmentrepo.findAllOrderByNameAsc();
        submissionAgreement.setDropdownDepartments(df);

        model.addAttribute(SSAS_FORM, submissionAgreement);

        model.addAttribute("action", "EditSsa");
        return "EditSsa";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/DeleteCopyright", method = RequestMethod.GET)
    public String DeleteCopyright(
            @RequestParam(value = "id", required = false) int id,
            @RequestParam(value = "ssaid", required = false) int ssaid,
            ModelMap model,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DeleteCopyright Get");

        if (id == 0) {
            LOGGER.log(Level.SEVERE, "DeleteCopyright Get id={0}", new Object[]{id});
            model.addAttribute("action", "EditSsa");
            return "EditSsa";
        }
        if (ssaid == 0) {
            LOGGER.log(Level.SEVERE, "DeleteCopyright Get ssaid={0}", new Object[]{ssaid});
            model.addAttribute("action", "EditSsa");
            return "EditSsa";
        }

        SsaCopyrightsForm cr = copyrightrepo.findById(id);
        copyrightrepo.delete(cr);

        LOGGER.log(Level.INFO, "delete ssaCopyrightsForms: copyrightid={0}", new Object[]{id});

        SubmissionAgreement submissionAgreement = ssarepo.findById(ssaid);

        List<SsaCopyrightsForm> crs = submissionAgreement.getSsaCopyrightsForms();
        List<SsaCopyrightsForm> newcrs = new ArrayList<SsaCopyrightsForm>();
        for (SsaCopyrightsForm crf : crs) {
            if (crf.getId() != id) {
                newcrs.add(crf);
            }
        }
        submissionAgreement.setSsaCopyrightsForms(newcrs);

        List<Department> df = departmentrepo.findAllOrderByNameAsc();
        submissionAgreement.setDropdownDepartments(df);
        model.addAttribute(SSAS_FORM, submissionAgreement);


        model.addAttribute("action", "EditSsa");
        return "EditSsa";
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/DeleteRsasForSsa")
    public String DeleteRsasForSsa(ModelMap model,

                                   @RequestParam("ssaid") int ssaid,
                                   @RequestParam(value = "rsa", required = false) int[] deletersas,

                                   HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DeleteRsasForSsa");

        if (ssaid > 0) {
            session.setAttribute("ssaid", ssaid);
        } else {
            ssaid = (Integer) session.getAttribute("ssaid");
        }
        model.addAttribute("ssaid", ssaid);

        LOGGER.log(Level.INFO, "ssaid={0}", new Object[]{ssaid});

        if (deletersas == null) {
            model.addAttribute("nodeletes", "1");
        } else {

            String dropoff = env.getRequiredProperty("dropoff.dir");
            if (dropoff == null || dropoff.equals("")) {
                LOGGER.log(Level.SEVERE, "dropoff is null");
                return "Home";
            }


            String sep = "";
            int cnt = 0;
            StringBuilder sb = new StringBuilder();
            for (int rsaid : deletersas) {
                LOGGER.log(Level.INFO, "delete rsaid={0}", new Object[]{rsaid});

                sb.append(sep + rsaid);
                sep = ", ";

                TransferRequest rsa = rsarepo.findById(rsaid);

		/*
		List<RsaFileDataForm> fds =  rsa.getRsaFileDataForms();
		for(RsaFileDataForm fd : fds){
		    rsaFileDataFormRepository.delete(fd);
		}
		*/

                rsa.setDeleted(true);
                rsarepo.save(rsa);
                //rsarepo.delete(rsa);

                /*try {
                    FileUtils.deleteDirectory(new File(dropoff + "/" + rsaid));
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
*/
                cnt++;
            }
            String deletedrsas = sb.toString();
            model.addAttribute("deletedrsas", deletedrsas);
            if (cnt > 0) {
                model.addAttribute("weredeletes", "1");
            }

        }

        SubmissionAgreement ssa = ssarepo.findById(ssaid);
        if (ssa != null) {
            List<TransferRequest> rsas = ssa.getTransferRequests();

            model.addAttribute("rsas", rsas);

            if (rsas.size() == 0) {
                model.addAttribute(SSAS_FORM, ssa);
            }

            model.addAttribute("action", "EditSsa");
            return "EditSsa";

        } else {
            return "SsaDeleteWarning";
        }
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/DownloadSubmissionAgreementFormPDF")
    public void DownloadSubmissionAgreementFormPDF(
            ModelMap model,
            HttpServletRequest request,
            @RequestParam("ssaid") int ssaid,
            HttpServletResponse response,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DownloadSubmissionAgreementFormPDF");

        if (ssaid <= 0) {
            LOGGER.log(Level.SEVERE, "DownloadSubmissionAgreementFormPDF Error: ssaid <= 0");
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

/*
        CreatePdf cp = new CreatePdf();
*/

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

      /*  String htmlstring = VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, "velocity/Request.vm", "UTF8", vemodel);

        cp.downloadpdf(response, htmlstring);*/
    }

    // ------------------------------------------------------------------------
    @ResponseBody
    @RequestMapping("/CheckIfSsaApproved")
    public String CheckIfSsaApproved(
            ModelMap model,
            @RequestParam(value = "ssaid", required = false) int ssaid,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "CheckIfSsaApproved");

        if (ssaid <= 0) {
            LOGGER.log(Level.INFO, "CheckIfSsaApproved: ssaid problem ssaid={0}", new Object[]{ssaid});
            return "invalid";
        }

        SubmissionAgreement ssa = ssarepo.findById(ssaid);
        if (ssa == null) {
            return "invalid";
        }

        boolean approved = ssa.isApproved();
        LOGGER.log(Level.INFO, "CheckIfSsaApproved: returning approved={0} for ssaid={1}", new Object[]{approved, ssaid});

        if (approved) return "true";
        return "false";
    }

    // ------------------------------------------------------------------------
    @RequestMapping(value = "/DepartmentDeleteWarningDeleteSsas", method = RequestMethod.POST)
    public String DepartmentDeleteWarningDeleteSsas(
            ModelMap model,
            @RequestParam("departmentid") int departmentid,
            @RequestParam(value = "ssaids", required = false) int[] ssaids,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DepartmentDeleteWarningDeleteSsas Post");

        if (departmentid <= 0) {
            departmentid = (Integer) session.getAttribute("departmentid");
        } else {
            session.setAttribute("departmentid", departmentid);
        }
        model.addAttribute("departmentid", departmentid);


        if (ssaids == null) {
            model.addAttribute("nossadeletes", "1");
        } else {
            StringBuilder sb1 = new StringBuilder();
            String sep1 = "";
            StringBuilder sb2 = new StringBuilder();
            String sep2 = "";
            int deletedcnt = 0;
            int rejectedcnt = 0;
            for (int ssaid : ssaids) {

                SubmissionAgreement ssa = ssarepo.findById(ssaid);
                if (ssa != null) {
                    List<TransferRequest> rsas = ssa.getTransferRequests();

                    if (rsas == null || rsas.isEmpty()) {

                        List<SsaContactsForm> cfs = ssa.getSsaContactsForms();
                        if (cfs != null) {
                            for (SsaContactsForm cf : cfs) {
                                contactrepo.delete(cf);
                            }
                        }

                        List<SsaCopyrightsForm> crfs = ssa.getSsaCopyrightsForms();
                        if (crfs != null) {
                            for (SsaCopyrightsForm crf : crfs) {
                                copyrightrepo.delete(crf);
                            }
                        }

                        List<SsaAccessRestrictionsForm> rfs = ssa.getSsaAccessRestrictionsForms();
                        if (rfs != null) {
                            for (SsaAccessRestrictionsForm rf : rfs) {
                                accessrestrictionrepo.delete(rf);
                            }
                        }

                        List<SsaFormatTypesForm> fts = ssa.getSsaFormatTypesForms();
                        if (fts != null) {
                            for (SsaFormatTypesForm ff : fts) {
                                formattyperepo.delete(ff);
                            }
                        }

                        ssa.setDeleted(true);
                        ssarepo.save(ssa);
                        //ssarepo.delete(ssa);

                        sb1.append(sep1 + ssaid);
                        sep1 = ", ";
                        deletedcnt++;
                    } else {
                        sb2.append(sep2 + ssaid);
                        sep2 = ", ";
                        rejectedcnt++;
                    }
                }
            }
            String deletedssas = sb1.toString();
            String rejectedssas = sb2.toString();

            if (deletedcnt > 0) {
                model.addAttribute("somessasdeleted", "1");
                model.addAttribute("deletedssas", deletedssas);
            }
            if (rejectedcnt > 0) {
                model.addAttribute("somessasnotdeleted", "1");
                model.addAttribute("rejectedssas", rejectedssas);
            }
        }

        Department df = departmentrepo.findById(departmentid);

        List<User> users = df.getUsers();
        model.addAttribute("users", users);

        List<SubmissionAgreement> ssas = ssarepo.findAllForDepartmentId(departmentid);
        model.addAttribute("ssas", ssas);

        if (users.isEmpty() && ssas.isEmpty()) {
            model.addAttribute("departmentid", departmentid);
            model.addAttribute("departmentname", df.getName());
            model.addAttribute("dependenciesresolved", "1");
            model.addAttribute("dependencies", "0");
            return "EditDepartment";
        }

        return "DepartmentDeleteWarning";
    }

    // ------------------------------------------------------------------------
    @RequestMapping("/DepartmentDeleteWarningDeleteSsas")
    public String DepartmentDeleteWarningDeleteSsas(
            ModelMap model,
            @RequestParam("departmentid") int departmentid,
            HttpSession session
    ) {
        LOGGER.log(Level.INFO, "DepartmentDeleteWarningDeleteSsas");

        if (departmentid <= 0) {
            departmentid = (Integer) session.getAttribute("departmentid");
        } else {
            session.setAttribute("departmentid", departmentid);
        }
        model.addAttribute("departmentid", departmentid);

        Department df = departmentrepo.findById(departmentid);

        List<User> users = df.getUsers();
        model.addAttribute("users", users);

        List<SubmissionAgreement> ssas = ssarepo.findAllForDepartmentId(departmentid);
        model.addAttribute("ssas", ssas);

        if (users.isEmpty() && ssas.isEmpty()) {
            model.addAttribute("departmentid", departmentid);
            model.addAttribute("departmentname", df.getName());
            model.addAttribute("dependenciesresolved", "1");
            model.addAttribute("dependencies", "0");
            return "EditDepartment";
        }

        return "DepartmentDeleteWarning";
    }
}
