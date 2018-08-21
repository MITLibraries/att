package edu.mit.att.service;

import edu.mit.att.entity.ApprovedRsasForm;
import edu.mit.att.entity.RsasForm;
import edu.mit.att.entity.SsaContactsForm;
import edu.mit.att.repository.ApprovedRsasFormRepository;
import edu.mit.att.repository.SsaContactsFormRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ApprovedRsasFormServiceImpl implements ApprovedRsasFormService {
    private final static Logger LOGGER = Logger.getLogger(ApprovedRsasFormServiceImpl.class.getCanonicalName());

    @Resource
    private ApprovedRsasFormRepository approvedrsarepo;

    @Resource
    private SsaContactsFormRepository contactrepo;

    // ------------------------------------------------------------------------
    @Transactional
    public String findAllApprovedTransfersCSV() {

        List<ApprovedRsasForm> ats = approvedrsarepo.findAllOrderByDatetimeAsc();
        if (ats == null) {
            LOGGER.log(Level.SEVERE, "findAllOrderByDatetimeAsc null");
            return null;
        }

        String csvstring = "\"Date/Time\", \"Accession Number\", \"Transfer No.\",  \"Department/Unit Name\", \"Dept Head Name\", \"Dept Head Email\", \"Approver Name\", \"Approver Email\", \"Description\"\r\n";

        for (ApprovedRsasForm at : ats) {
            csvstring += "\"" + at.getDatetime().replace(".0", "") + "\", ";
            csvstring += "\"" + at.getAccession() + "\", ";
            csvstring += "\"" + at.getRsaid() + "\", ";
            csvstring += "\"" + at.getDepartmentname() + "\", ";
            csvstring += "\"" + at.getSendername() + "\", ";
            csvstring += "\"" + at.getSenderemail().replace("\"", "\\\"") + "\", ";
            csvstring += "\"" + at.getApprovername() + "\", ";
            csvstring += "\"" + at.getApproveremail().replace("\"", "\\\"") + "\", ";
            csvstring += "\"" + at.getDescription() + "\"\r\n";
        }
        return csvstring;
    }

    // ------------------------------------------------------------------------
    @Transactional
    public void recordDeletedRsa(RsasForm rsa, HttpSession session) {
        if (rsa == null || rsa.getSubmissionAgreement() == null || rsa.getSubmissionAgreement().getDepartment() == null || rsa.getSubmissionAgreement().getDepartment().getName() == null) {
            return;
        }

        String sqldatetimenow = String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS", Calendar.getInstance());

        String departmentname = rsa.getSubmissionAgreement().getDepartment().getName();

        String sendername = "";
        if (rsa.getSubmissionAgreement() != null && rsa.getSubmissionAgreement().getDepartmenthead() != null) {
            sendername = rsa.getSubmissionAgreement().getDepartmenthead();
        }

        String senderemail = "";

        int ssaid = rsa.getSubmissionAgreement().getId();
        if (ssaid > 0) {

            List<SsaContactsForm> cs = contactrepo.findAllBySsaIdOrderByNameAsc(ssaid);

            if (cs != null) {
                StringBuilder sb = new StringBuilder();
                String sep = "";
                for (SsaContactsForm contact : cs) {
                    sb.append(sep + "\"" + contact.getName() + "\" <" + contact.getEmail() + ">");
                    sep = ", ";
                }
                senderemail = sb.toString();
            }
        }

        ApprovedRsasForm arf = new ApprovedRsasForm();
        arf.setDatetime(sqldatetimenow);
        arf.setAccession(rsa.getAccessionnumber());
        arf.setRsaid(rsa.getId());
        if (departmentname == null) {
            LOGGER.log(Level.SEVERE, "Department name is null for rasid={0}", new Object[]{rsa.getId()});
            arf.setDepartmentname("");
        } else {
            arf.setDepartmentname(departmentname);
        }

        arf.setSendername(sendername);
        arf.setSenderemail(senderemail);

        // FIXME -- who's approved it?

        arf.setApprovername("test");
        arf.setApproveremail("test@mit.edu");

        arf.setDescription(rsa.getDescription());
        approvedrsarepo.save(arf);
    }
}
