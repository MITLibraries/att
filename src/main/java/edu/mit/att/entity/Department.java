package edu.mit.att.entity;

import javax.persistence.*;
import java.util.List;

import lombok.*;

@Data
@Entity
@Table(name = "departments")
@EqualsAndHashCode(exclude = {"submissionAgreement", "users"})
@ToString(exclude = {"users", "submissionAgreement"})
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String name;

    //@Transient
    //private boolean active = true; // used for usersForm

    @OneToOne(fetch = FetchType.EAGER, mappedBy = "department")
    private SubmissionAgreement submissionAgreement;

    public int hashCode(SubmissionAgreement sf) {
        return sf.getId();
    }

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "map",
            joinColumns = @JoinColumn(name = "departmentid", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "userid", referencedColumnName = "id")
    )
    private List<User> users;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Department that = (Department) o;

        if (id != that.id) return false;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public int getId() {
        return id;
    }
}
