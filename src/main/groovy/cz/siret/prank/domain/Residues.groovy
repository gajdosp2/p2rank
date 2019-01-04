package cz.siret.prank.domain

import com.google.common.collect.Maps
import cz.siret.prank.geom.Atoms
import cz.siret.prank.geom.Struct
import groovy.transform.CompileStatic
import org.biojava.nbio.structure.Atom
import org.biojava.nbio.structure.Group

import javax.annotation.Nullable

/**
 *
 */
@CompileStatic
class Residues implements Iterable<Residue> {

    private List<Residue> list
    private Atoms atoms
    private Map<Residue.Key, Residue> indexByKey

    Residues(List<Residue> list) {
        this.list = list
        this.indexByKey = Maps.uniqueIndex(list, { it.key })
    }

    List<Residue> getList() {
        return list
    }

    Atoms getAtoms() {
        if (atoms == null) {
            atoms = Atoms.join(list.collect { it.atoms })
        }
        atoms
    }

    boolean contains(Residue residue) {
        indexByKey.containsKey(residue.key)
    }

    int getCount() {
        list.size()
    }

    @Nullable
    Residue getResidue(Residue.Key key) {
        indexByKey.get(key)
    }

    @Nullable
    Residue getResidueForAtom(Atom a) {
        getResidue(Residue.Key.forAtom(a) as Residue.Key)
    }

    @Nullable
    Residue getResidueForGroup(Group g) {
        getResidue(Residue.Key.forAtom(g?.atoms?.first()) as Residue.Key)
    }

    List<Residue> getDistinctForAtoms(Atoms atoms) {
        atoms.distinctGroups.collect { getResidueForGroup(it) }.findAll{ it != null }.asList()
    }

    Residue findNearest(Atom point) {
        Atom nearestAtom = getAtoms().withKdTree().kdTree.findNearest(point)
        return getResidueForAtom(nearestAtom)
    }

    @Override
    Iterator<Residue> iterator() {
        return list.iterator()
    }

}