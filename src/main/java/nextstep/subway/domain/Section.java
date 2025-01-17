package nextstep.subway.domain;

import lombok.Builder;
import lombok.Getter;
import nextstep.subway.exception.CustomException;
import nextstep.subway.exception.code.CommonCode;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.List;
import java.util.Objects;

@Getter
@Entity
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "line_id")
    private Line line;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "up_station_id")
    private Station upStation;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "down_station_id")
    private Station downStation;

    private int distance;

    public Section() {
    }

    @Builder
    public Section(Line line, Station upStation, Station downStation, int distance) {
        this.line = line;
        this.upStation = upStation;
        this.downStation = downStation;
        this.distance = distance;
    }

    public boolean hasSameUpStation(Station station) {
        return upStation.equals(station);
    }

    public boolean hasSameDownStation(Station station) {
        return downStation.equals(station);
    }

    public List<Station> getAllStation() {
        return List.of(upStation, downStation);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Section section = (Section) o;
        return Objects.equals(id, section.id) &&
               Objects.equals(line, section.line) &&
               Objects.equals(upStation, section.upStation) &&
               Objects.equals(downStation, section.downStation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(line, upStation, downStation);
    }

    public void setLine(final Line line) {
        this.line = line;
    }

    public boolean hasStation(final Station station) {
        return downStation.equals(station) || upStation.equals(station);
    }
}