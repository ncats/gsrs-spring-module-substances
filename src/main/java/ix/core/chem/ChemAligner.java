package ix.core.chem;

import java.util.stream.IntStream;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.SingularValueDecomposition;
import gov.nih.ncats.molwitch.Atom;
import gov.nih.ncats.molwitch.AtomCoordinates;
import gov.nih.ncats.molwitch.Bond;
import gov.nih.ncats.molwitch.Bond.Stereo;
import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.MolwitchException;

public class ChemAligner {
    private ChemAligner () {
    }
    
    /**
     * Clean the molecule, generating new coordinates but 
     * also attempting to rotate the cleaned version to be 
     * as close as possible to the old version.
     * 
     * This mutates the chemical object, and returns it 
     * as well.
     * @param c
     */
    public static Chemical align2DClean(Chemical c) {
        Chemical q=c.copy();
        Chemical t=c;
        try{
            t.generateCoordinates();
            align(q,t, IntStream.range(0, t.getAtomCount()).toArray());
        }catch(Exception e) {
            //?
        }
        return t;
        
        
    }
    /**
     * Perform aligment of the target molecule given the query molecule
     * and the atom mappings.  The map array is indexed by the atom index
     * of ref, i.e., map[i] = j implies that the ith atom of query corresponds
     * to the jth atom of target where j >= 0, otherwise there is no mapping.
     */
    public static void align (Chemical query, Chemical target, int[] map) {
        if (query.getAtomCount() != map.length) {
            throw new IllegalArgumentException 
            ("Input mapping does match reference molecule size");
        }
        if (!query.has2DCoordinates()) {
            try {
                query.generateCoordinates();
            } catch (MolwitchException e) {
                throw new RuntimeException(e);
            }
        }
        int size = 0;
        double qx = 0., qy = 0., qz = 0.;
        double tx = 0., ty = 0., tz = 0.;
        Atom[] qatoms = query.atoms().toArray(i->new Atom[i]);
        Atom[] tatoms = target.atoms().toArray(i->new Atom[i]);
        for (int i = 0; i < map.length; ++i) {
            if (map[i] < 0) {
            }
            else {
                Atom q = qatoms[i];
                Atom t = tatoms[map[i]];
                qx += q.getAtomCoordinates().getX();
                qy += q.getAtomCoordinates().getY();
                qz += q.getAtomCoordinates().getZ().orElse(0);
                tx += t.getAtomCoordinates().getX();
                ty += t.getAtomCoordinates().getY();
                tz += t.getAtomCoordinates().getZ().orElse(0);
                ++size;
            }
        }
        qx /= size;
        qy /= size;
        qz /= size;
        tx /= size;
        ty /= size;
        tz /= size;
        // now center the vectors
        DoubleMatrix2D Y = DoubleFactory2D.dense.make(3, size);
        DoubleMatrix2D X = DoubleFactory2D.dense.make(3, size);
        for (int i = 0, j = 0; i < map.length; ++i) {
            if (map[i] < 0) {
            }
            else {
                Atom q = qatoms[i];
                Atom t = tatoms[map[i]];
                X.setQuick(0, j, q.getAtomCoordinates().getX() - qx);
                X.setQuick(1, j, q.getAtomCoordinates().getY() - qy);
                X.setQuick(2, j, q.getAtomCoordinates().getZ().orElse(0) - qz);
                Y.setQuick(0, j, t.getAtomCoordinates().getX() - tx);
                Y.setQuick(1, j, t.getAtomCoordinates().getY() - ty);
                Y.setQuick(2, j, t.getAtomCoordinates().getZ().orElse(0) - tz);
                ++j;
            }
        }
        //System.out.println("Y = " + Y);
        //System.out.println("X = " + X);
        Algebra alg = new Algebra ();
        // now compute A = YX'
        DoubleMatrix2D A = alg.mult(Y, alg.transpose(X));	
        //System.out.println("A = " + A);
        SingularValueDecomposition svd = new SingularValueDecomposition(A);

        DoubleMatrix2D U = svd.getU();
        DoubleMatrix2D V = svd.getV();
        DoubleMatrix2D R = alg.mult(U, alg.transpose(V));
        //System.out.println("R = " + R);
        R = alg.transpose(R);
        for (Atom a : tatoms) {
            double x = a.getAtomCoordinates().getX();
            double y = a.getAtomCoordinates().getY();
            double z = a.getAtomCoordinates().getZ().orElse(0);
            double rx = x*R.getQuick(0, 0) 
                    + y*R.getQuick(0, 1) + z*R.getQuick(0, 2);
            double ry = x*R.getQuick(1, 0) 
                    + y*R.getQuick(1, 1) + z*R.getQuick(1, 2);
            double rz = x*R.getQuick(2, 0) 
                    + y*R.getQuick(2, 1) + z*R.getQuick(2, 2);
            a.setAtomCoordinates(AtomCoordinates.valueOf(rx, ry));
            //	    .setXYZ(rx, ry, rz);
        }
        //if rotoinversion, invert dashes and wedges
        double det = R.getQuick(0, 0)*R.getQuick(1, 1)-R.getQuick(0, 1)*R.getQuick(1, 0);
        if(det<0){
            for(Bond mb:target.getBonds()){
                if(mb.getBondType().getOrder()==1 && !mb.getStereo().equals(Stereo.NONE)){
                    mb.setStereo(mb.getStereo().flip());
                }
            }
        }
    }
    public static void main (String[] argv) throws Exception {
        Chemical query=Chemical.parseMol(
                "\r\n" + 
                        "   JSDraw203312114452D\r\n" + 
                        "\r\n" + 
                        "  9  9  0  0  0  0              0 V2000\r\n" + 
                        "   14.0920  -10.0360    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\r\n" + 
                        "   12.7410   -9.2560    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\r\n" + 
                        "   12.7410   -7.6960    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\r\n" + 
                        "   15.4430   -9.2560    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\r\n" + 
                        "   15.4430   -7.6960    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\r\n" + 
                        "   14.0920   -6.9160    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\r\n" + 
                        "   16.7940   -8.4760    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\r\n" + 
                        "   16.7940  -10.0360    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\r\n" + 
                        "   16.7940  -11.5960    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\r\n" + 
                        "  1  2  2  0  0  0  0\r\n" + 
                        "  2  3  1  0  0  0  0\r\n" + 
                        "  1  4  1  0  0  0  0\r\n" + 
                        "  4  5  2  0  0  0  0\r\n" + 
                        "  5  6  1  0  0  0  0\r\n" + 
                        "  6  3  2  0  0  0  0\r\n" + 
                        "  5  7  1  0  0  0  0\r\n" + 
                        "  7  8  1  0  0  0  0\r\n" + 
                        "  8  9  1  0  0  0  0\r\n" + 
                        "M  END"
                );
        Chemical target = Chemical.parseMol(
                "\n"
                + "   JSDraw203312116242D\n"
                + "\n"
                + " 11 11  0  0  0  0              0 V2000\n"
                + "   24.8013   -8.9801    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   24.0483  -10.3462    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   22.4967  -10.3817    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   24.0029   -7.6493    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   22.4422   -7.6797    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   21.6892   -9.0459    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   21.6358   -6.3443    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   22.3891   -4.9782    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   21.5827   -3.6428    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   21.7460  -11.7492    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   24.0483  -11.9062    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "  1  2  2  0  0  0  0\n"
                + "  2  3  1  0  0  0  0\n"
                + "  1  4  1  0  0  0  0\n"
                + "  4  5  2  0  0  0  0\n"
                + "  5  6  1  0  0  0  0\n"
                + "  6  3  2  0  0  0  0\n"
                + "  5  7  1  0  0  0  0\n"
                + "  7  8  1  0  0  0  0\n"
                + "  8  9  1  0  0  0  0\n"
                + "  3 10  1  0  0  0  0\n"
                + "  2 11  1  0  0  0  0\n"
                + "M  END\n"
                + ""
                );
        //    	target.aromatize();
        query.setName("Query1");
        //    	query.aromatize();
//        int[] map = IntStream.range(0,9).toArray();
        ChemAligner.align2DClean(target);
        System.out.println(target.toMol());
    }
}