package se325.assignment01.concert.service.domain;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import se325.assignment01.concert.common.jackson.LocalDateTimeDeserializer;
import se325.assignment01.concert.common.jackson.LocalDateTimeSerializer;

/**
 * This is a Concert Class which stores all the information related to a Concert
 * This Class will be persisted into a relational database.
 */

@Entity
@Table(name = "CONCERTS")
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String image_name;

    @Column(name = "BLURB", length = 1024)
    private String blurb;

    // Map concert_date Collection to new Table
    @ElementCollection
    @CollectionTable(
            name = "CONCERT_DATES",
            joinColumns = @JoinColumn(name = "CONCERT_ID"))
    @Column(name = "DATE")
    private Set<LocalDateTime> concert_dates = new HashSet<>();

    // Map performer Collection to new table. There is a many to many relationship between concert and performer
    // pre-fetching (SUBSELECT) to solve n + 1 problem
    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    @JoinTable(
            name = "CONCERT_PERFORMER",
            joinColumns = @JoinColumn(name = "CONCERT_ID"),
            inverseJoinColumns = {@JoinColumn(name = "PERFORMER_ID")})
    private Set<Performer> performers = new HashSet<>();

    // Default Constructor
    public Concert() {}

    public Concert(Long id, String title, String imageName, String blurb) {
        this.id = id;
        this.title = title;
        this.image_name = imageName;
        this.blurb = blurb;
    }

    public Long getID() {
        return id;
    }

    public void setID(Long id) {
        this.id = id;
    }

    public void addPerformer(Performer performer) {
        performers.add(performer);
    }

    public void setPerformers(Set<Performer> performers) {
        this.performers = performers;
    }

    public Set<Performer> getPerformers() {
        return performers;
    }

    public void addDate(LocalDateTime date) {
        this.concert_dates.add(date);
    }

    public void setConcert_dates(Set<LocalDateTime> concert_dates) {
        this.concert_dates = concert_dates;
    }

    public Set<LocalDateTime> getDates() {
        return concert_dates;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage_name() {
        return image_name;
    }

    public void setImage_name(String image_name) {
        this.image_name = image_name;
    }

    public String getBlurb() {
        return blurb;
    }

    public void setBlurb(String blurb) {
        this.blurb = blurb;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Concert))
            return false;
        if (obj == this)
            return true;

        Concert rhs =(Concert) obj;
        return new EqualsBuilder().append(id, rhs.getID()).append(title, rhs.getTitle()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(id).append(title).toHashCode();
    }
}
