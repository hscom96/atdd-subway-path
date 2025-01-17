package nextstep.subway.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nextstep.subway.exception.CustomException;
import nextstep.subway.exception.code.CommonCode;
import nextstep.subway.exception.code.SectionCode;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class Sections {
    public static final int INVALID_REMOVE_SIZE = 1;

    @OneToMany(mappedBy = "line", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private final List<Section> sections = new ArrayList<>();

    public void add(final Section section) {
        if (sections.isEmpty()) {
            sections.add(section);
            return;
        }

        validNotExistStation(section);
        validExistAllStation(section);

        addSectionIfNotBetween(section);
        addSectionIfBetween(section);
    }

    private void validNotExistStation(final Section section) {
        if (!hasStation(section.getDownStation()) && !hasStation(section.getUpStation())) {
            throw new CustomException(CommonCode.PARAM_INVALID);
        }
    }

    private void validExistAllStation(final Section section) {
        if (hasStation(section.getDownStation()) && hasStation(section.getUpStation())) {
            throw new CustomException(CommonCode.PARAM_INVALID);
        }
    }

    private void addSectionIfNotBetween(final Section section) {
        Station upEndStation = getUpEndStation();
        Station downEndStation = getDownEndStation();
        if (upEndStation.equals(section.getDownStation()) || downEndStation.equals(section.getUpStation())) {
            sections.add(section);
        }
    }

    private void addSectionIfBetween(final Section section) {
        Section matchSection = getSectionHasSameStation(section).orElse(null);
        if (matchSection == null || matchSection.equals(section)) {
            return;
        }
        if (matchSection.getDistance() <= section.getDistance()) {
            throw new CustomException(CommonCode.PARAM_INVALID);
        }
        Station upStation = matchSection.hasSameDownStation(section.getDownStation()) ?
                            matchSection.getUpStation() :
                            section.getDownStation();
        Station downStation = matchSection.hasSameDownStation(section.getDownStation()) ?
                              section.getUpStation() :
                              matchSection.getDownStation();
        int distance = matchSection.getDistance() - section.getDistance();
        sections.add(new Section(matchSection.getLine(), upStation, downStation, distance));
        sections.add(section);
        sections.remove(matchSection);
    }

    public void removeSection(final Station station) {
        validInvalidRemoveSize();
        validStationExist(station);

        removeIfNotBetween(station);
        removeIfBetween(station);
    }

    private void validStationExist(final Station station) {
        if(!hasStation(station)){
            throw new CustomException(CommonCode.PARAM_INVALID);
        }
    }

    private void validInvalidRemoveSize() {
        if (size() <= INVALID_REMOVE_SIZE) {
            throw new CustomException(SectionCode.SECTION_REMOVE_INVALID);
        }
    }

    private void removeIfNotBetween(final Station station) {
        Optional<Section> upEndSection = getUpEndSection();
        if(upEndSection.isPresent() && station.equals(upEndSection.get().getUpStation())){
            sections.remove(upEndSection.get());
        }

        Optional<Section> downEndSection = getDownEndSection();
        if(downEndSection.isPresent() && station.equals(downEndSection.get().getDownStation())){
            sections.remove(downEndSection.get());
        }
    }

    private void removeIfBetween(final Station station) {
        Optional<Section> beforeSection = getSectionHasSameDownStation(station);
        Optional<Section> afterSection = getSectionHasSameUpStation(station);

        if (afterSection.isEmpty() || beforeSection.isEmpty()){
            return;
        }

        int newDistance = afterSection.get().getDistance() + beforeSection.get().getDistance();
        sections.add(new Section(beforeSection.get().getLine(), beforeSection.get().getUpStation(), afterSection.get().getDownStation(), newDistance));
        sections.remove(beforeSection.get());
        sections.remove(afterSection.get());
    }

    public int size() {
        return sections.size();
    }

    public boolean isEmpty() {
        return sections.isEmpty();
    }

    public Station getDownEndStation() {
        Set<Station> stations = new HashSet<>(getStations());
        for (Section section : getSections()) {
            stations.remove(section.getUpStation());
        }
        return new ArrayList<>(stations).get(0);
    }

    public Station getUpEndStation() {
        Set<Station> stations = new HashSet<>(getStations());
        for (Section section : getSections()) {
            stations.remove(section.getDownStation());
        }
        return new ArrayList<>(stations).get(0);
    }

    public Optional<Section> getUpEndSection(){
        return getSectionHasSameUpStation(getUpEndStation());
    }

    public Optional<Section> getDownEndSection(){
        return getSectionHasSameDownStation(getDownEndStation());
    }

    public List<Station> getStations() {
        return getSections().stream()
                            .map(Section::getAllStation)
                            .flatMap(List::stream)
                            .distinct()
                            .collect(Collectors.toUnmodifiableList());
    }


    public List<Station> getStationsSorted() {
        return getSectionsSorted().stream()
                                  .map(Section::getAllStation)
                                  .flatMap(List::stream)
                                  .distinct()
                                  .collect(Collectors.toUnmodifiableList());
    }

    public List<Section> getSectionsSorted() {
        List<Section> result = new ArrayList<>();
        Optional<Section> section = getSectionHasSameUpStation(getUpEndStation());
        while (section.isPresent()) {
            result.add(section.get());
            section = getSectionHasSameUpStation(section.get().getDownStation());
        }
        return result;
    }

    public List<Section> getSections() {
        return new ArrayList<>(sections);
    }

    public List<String> getStationNames() {
        return getStationsSorted().stream()
                                  .map(Station::getName)
                                  .collect(Collectors.toList());
    }

    private Optional<Section> getSectionHasSameStation(final Section section) {
        Section matchSection = getSectionHasSameUpStation(section.getUpStation())
            .orElse(getSectionHasSameDownStation(section.getDownStation())
                        .orElse(null));
        return Optional.ofNullable(matchSection);
    }

    private Optional<Section> getSectionHasSameUpStation(final Station station) {
        return sections.stream()
                       .filter(section -> section.hasSameUpStation(station))
                       .findFirst();
    }

    private Optional<Section> getSectionHasSameDownStation(final Station station) {
        return sections.stream()
                       .filter(section -> section.hasSameDownStation(station))
                       .findFirst();
    }

    private boolean hasStation(Station station) {
        return sections.stream()
                       .anyMatch(section -> section.hasStation(station));
    }
}
