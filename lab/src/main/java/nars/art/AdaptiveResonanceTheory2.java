


package nars.art;

import java.util.ArrayList;
import java.util.Random;

/**
 * code from https:
 * 
 * 
 */
public class AdaptiveResonanceTheory2   
{
    /**
             * Count score (similarity) how similar inst and prot are.
             * The output (similarity) will be their dot product 
             * \param inst it is some instance (example, Ek)
             * \param prot it is some prototype
             **/
    private static float countScore(DynamicVector<Float> prototype, DynamicVector<Float> instance) {
        int i;
        float score = 0.0f;
        for (i = 0;i < prototype.array.length;i++)
        {
            score += instance.get___idx(i) * prototype.get___idx(i);
        }
        return score;
    }

    /**
             * Add an example (Ek) to a particular cluster.
             * It means that it moves the prototype toward the example.
             * The prototype will be more similar with the example.
             * P'=sqrt( sum_i((1-beta)*Pi + beta*Eki)^2 )
             * \param inst Ek
             * \param prot some prototype
             * \param beta it is given by an user
             * */
    private static void addInstance(DynamicVector<Float> instance, DynamicVector<Float> prototype, float beta) {
        DynamicVector<Float> temp;
        int i;


        float norm = 0.0f;
        
        try {
            temp = new DynamicVector<>(prototype.array.length);
        }
        catch( Exception ex ) {
            throw new RuntimeException("array ctor exception");
        }
        
        for (i = 0;i < instance.array.length;i++)
        {
            
            temp.set___idx(i,(1.0f - beta) * prototype.get___idx(i) + beta * instance.get___idx(i));
        }
        for (i = 0;i < instance.array.length;i++)
        {
            
            norm += temp.get___idx(i) * temp.get___idx(i);
        }
        norm = (float) Math.sqrt(norm);
        
        norm = 1.0f / norm;
        for (i = 0;i < instance.array.length;i++)
        {
            
            prototype.set___idx(i,norm * temp.get___idx(i));
        }
    }

    /**
             * Removing an instance(Ek) from the particular prototype.
             * Remove the instance with index 'iinst' in 'sample' from prototype
             * with index 'iprot' in 'prot'. But also remove particular index 
             * from prototype sequence
             **/
    private static void removeInstance(ArrayList<DynamicVector<Float>> sample, int iinst, ArrayList<DynamicVector<Float>> prot, int iprot, ArrayList<ArrayList<Integer>> seq, float beta, float vigilance) {
        int i;
        for (i = 0;i < seq.get(iprot).size();i++)
        {
            
            if (seq.get(iprot).get(i) == iinst)
            {
                seq.get(iprot).remove(i);
                break;
            }
             
        }
        
        
        if (seq.get(iprot).isEmpty())
        {
            prot.remove(iprot);
            seq.remove(iprot);
        }
        else
        {


            prot.set(iprot, sample.get(seq.get(iprot).get(0)));

            float score = countScore(sample.get(seq.get(iprot).get(0)), sample.get(seq.get(iprot).get(0)));
            if (score < vigilance)
            {
                float tmpv = vigilance;
                vigilance = score;
            }
             
            for (i = 1;i < seq.get(iprot).size();i++)
            {
                
                
                
                
                addInstance(sample.get(seq.get(iprot).get(i)), prot.get(iprot), beta);
            }
        } 
    }

    /**
             * Create a new prototype and also create a new sequence in prot_seq
             * One line in prot_seq = one cluster represented by a prototype
             * \param inst set of all examples
             * \param iinst ID of particular Ek
             * \param prot set of prototypes
             * \param prot_seq set of all prototypes with indexes of member's Ek
             * \param vigilance it is set by an user
             * */
    private static void createPrototype(ArrayList<DynamicVector<Float>> inst, int iinst, ArrayList<DynamicVector<Float>> prot, ArrayList<ArrayList<Integer>> prot_seq, float vigilance) {

        float score = countScore(inst.get(iinst), inst.get(iinst));
        if (score < vigilance)
        {
            float tmpv;
            if ((score - vigilance) < 0.0001f)
            {
                tmpv = vigilance;
                vigilance = vigilance - (0.0001f + 0.0001f * vigilance);
            }
            else
            {
                tmpv = vigilance;
                vigilance = score;
            } 
            
            
            int x = 0;
        }
         
        
        
        prot.add(inst.get(iinst));

        ArrayList<Integer> new_seq = new ArrayList<>();
        new_seq.add(iinst);
        prot_seq.add(new_seq);
    }

    /** 
             * Returns a prototype with highest similarity (score) -- which was not used yet.
             * The score is counted for a particular instance Ek and all the prototypes.
             * If it is returned empty prototype -- was not possible (for some rule) to find the best
             * @param inst example Ek
             * @param prot set of prototypes
             * @param used set of already tested prototypes
             **/
    private DynamicVector<Float> bestPrototype2A(DynamicVector<Float> inst, ArrayList<DynamicVector<Float>> prot, ArrayList<DynamicVector<Float>> used) {

        int i, j;
        ArrayList<DynamicVector<Float>> sameScore = new ArrayList<>();
        DynamicVector<Float> empty = new DynamicVector<>(0);

        int usize = used.size();
        int psize = prot.size();
        
        
        if (used.size() == prot.size())
        {
            return empty;
        }

        float[] score = new float[psize];
        for (i = 0;i < psize;i++)
        {
            
            score[i] = Float.MIN_VALUE;
        }
        for (i = 0;i < psize;i++)
        {

            boolean usedb = false;
            for (j = 0;j < usize;j++)
            {
                if (prot.get(i).equals(used.get(j)))
                {
                    usedb = true;
                    break;
                }
                 
            }
            
            if (usedb)
            {
            }
            else
            {
                
                score[i] = countScore(prot.get(i), inst);
            } 
        }

        float higher = Float.MIN_VALUE;
        for (i = 0;i < psize;i++)
        {
            if (score[i] == higher)
            {
                sameScore.add(prot.get(i));
            }
            else
            {
                if (score[i] > higher)
                {
                    
                    sameScore.clear();
                    sameScore.add(prot.get(i));
                    higher = score[i];
                }
                 
            } 
        }
        if (sameScore.isEmpty())
        {
            return empty;
        }
        else 
        if (sameScore.size() == 1)
        {
            return sameScore.get(0);
        }
        else
        {

            int index = random.nextInt(sameScore.size());
            return sameScore.get(index);
        }  
    }

    /**
         * In the structure Clust are stored all the results.<br>
         * More specifically: prototypes, fluctuation (error) and sequence
         * of all examples for each prototype
         **/
    public static class Clust   
    {
        /** 
                 * proto is a set of created prototypes
                 */
        public ArrayList<DynamicVector<Float>> proto = new ArrayList<>();
        /** 
                * proto_seq it is a sequence of sequences (a matrix). Where each line
                * represents one prototype and each column in the line represents some
                * example's ID.<br>
                * Example:<br>
                * 1 2 4<br>
                * 7 3 5
                *<br><br>
                * The first prototype consists of the ID's examples: 1, 2 and 3<br>
                * The second cluster consist of the following examples: 7, 3 and 5<br>
                * An example with ID 5 is a vector. In fact, it is an input line
                */
        public ArrayList<ArrayList<Integer>> proto_seq = new ArrayList<>();
        /** 
                * How many examples
                * were re-assign (they are in a different cluster then they were before) 
                * */
        public float fluctuation;
    }

    /**
         * In this structure are stored all input parameters important for run every art algorithm. 
         * They can be given by user or they are set as default
         **/
    public static class in_param   
    {
        /** 
                * Input parameter beta (-b) in ART 1 is a small positive integer. It influences a number of created clusters. The higher value the 
                * higher number of created clusters. The default is 1.<br>
                * For another ART implementations (based on real value input) <em>beta</em> is a learning constant. It has a range [0, 1]. The default
                * is 0.5
                * */
        public float beta;
        /**
               * positive integer or 0 - skip the last n columns (in input examples) -- default value:0
               * */
        int skip;
        /**
                * Input parameter vigilance (-v) together with alpha (ART for real numbers input) or beta (ART 1) set up a similarity threshold.
                * This threshold influence a minimum similarity under which an example Ek will be accepted by prototype. The higher value 
                * the higher number of clusters. It has a range [0,1]. The default is 0.1.
                * */
        public float vigilance;
        /** 
                * Input parameter theta (-t) denoising parameter. If a value Ek_i (Ek is a example and
                * it's ith column) is lower than theta then the number will be changed to 0. It is used only by ART 2A. It's range:
                * [0,1/dim^-0.5]. Default 0.00001
                * */
        float theta = 0.00001f;
        /** 
                * Input parameter alpha (-a)  is used by real value ART algorithms. Together with vigilance set up a similarity threshold.
                * This threshold influences a minimum similarity which is necessary for the example Ek to be accepted by the prototype. 
                * The range: [0,1/sqrt(dim)] where dim is a number of dimensions. The default is 1/sqrt(dim) * 1/2
                * */
        public float alpha;
        /** 
                * Input parameter distance (-d) set up a distance measure:
                *   <ol><li> Euclidean distance
                *    <li>Modified Euclidean distance -- it is in a testing mode. Euclidean distance use equation 1 - E/dim where E is Euclidean distance
                *    and dim is a number of dimensions. Modified Euclidean use equation log(dim^2) - E. This distance in some cases can achieve a better
                *    performance than standard Euclidean distance. However, it is recommended to use standard Euclidean distance. DO NOT USE IT
                *    <li> Manhattan distance
                *    <li> Correlation distance
                *    <li> Minkowski distance
                *   </ol>
                *    It works only for art_distance. Default Euclidean distance measure
                **/
        int distance;
        /** 
                * Input parameter power (-p) it is used only for Minkowski distance in art_distance. It set up the power for Minkowski
                * distance measure. The default is 3. Minkowski with the power 1 is Manhattan distance. Minkowski with power 2 is 
                * Euclidean distance
                * */
        int power;
        /**
                * An input parameter --  a number of passes (-E), it is a maximum number of how many times an example Ek 
                * can be re-assigned. If it reach this number the program will stop. The default is 100
                * */
        public int pass;
        /** 
                * An input parameter -- fluctuation (-e), it is a highest possible error rate (%). It means a maximum
                * number (in %) of how many instances can be re-assign. If the real fluctuatio is lower than -e
                * then program will stop. Default is 5% examples.
                * */
        public float error;
    }

    /**
             * ART 2A algorithm, inputs: examples and input parameters given by an user
             * How exactly it is working can be found at www.fi.muni.cz/~xhudik/art/drafts
             * \param sample  set if input examples (Eks)
             * \param par all input parameters set by an user or default
             **/
    public void art2A(ArrayList<DynamicVector<Float>> sample, in_param param, Clust results) {
        
        DynamicVector<Float> P;


        ArrayList<DynamicVector<Float>> used = new ArrayList<>();
        ArrayList<DynamicVector<Float>> prot = new ArrayList<>();
        ArrayList<ArrayList<Integer>> prot_seq = new ArrayList<>();
        ArrayList<DynamicVector<Float>> prot_best = new ArrayList<>();
        ArrayList<ArrayList<Integer>> prot_seq_best = new ArrayList<>();
        float fluctuation = 100.0f;
        
        
        float fluctuation_best = 120.0f;
        
        int pass = 0;

        int i, j;
        ArrayList<Boolean> changed = new ArrayList<>();
        for (i = 0;i < sample.size();i++)
        {
            changed.add(true);
        }
        while ((pass < param.pass) && (fluctuation > param.error))
        {

            for (i = 0;i < sample.size();i++)
            {
                
                changed.set(i, false);
            }
            for (i = 0;i < sample.size();i++)
            {
                
                
                used.clear();
                do
                {

                    P = bestPrototype2A(sample.get(i), prot, used);
                    
                    if (P.array.length == 0)
                    {

                        int prototypeIndex = Common.instanceInSequence(prot_seq, i);
                        if (prototypeIndex != -1)
                        {
                            
                            removeInstance(sample, i, prot, prototypeIndex, prot_seq, param.beta, param.vigilance);
                        }
                         
                        createPrototype(sample, i, prot, prot_seq, param.vigilance);
                        changed.set(i, true);
                        break;
                    }
                     
                    
                    used.add(P);

                    float score = countScore(P, sample.get(i));
                    float alphaSum = 0.0f;
                    for (j = 0;j < sample.get(i).array.length;j++)
                    {
                        alphaSum += param.alpha * sample.get(i).get___idx(j);
                    }
                    
                    if (score >= alphaSum)
                    {
                        if (score >= param.vigilance)
                        {

                            int prot_index = Common.instanceInSequence(prot_seq, i);
                            if (prot_index != -1)
                            {
                                
                                if (prot.get(prot_index).equals(P))
                                {
                                    break;
                                }
                                else
                                {
                                    
                                    removeInstance(sample, i, prot, prot_index, prot_seq, param.beta, param.vigilance);
                                } 
                            }


                            int Pindex = Common.findItem(prot, P, true);
                            
                            addInstance(sample.get(i), prot.get(Pindex), param.beta);
                            prot_seq.get(Pindex).add(i);
                            changed.set(i, true);
                            break;
                        }
                        else
                        {
                        }
                    }
                    else
                    {


                        int prot_index = Common.instanceInSequence(prot_seq, i);
                        if (prot_index != -1)
                        {
                            
                            removeInstance(sample, i, prot, prot_index, prot_seq, param.beta, param.vigilance);
                        }
                         
                        createPrototype(sample, i, prot, prot_seq, param.vigilance);
                        changed.set(i, true);
                        break;
                    } 
                }
                while (prot.size() != sample.size());
            }


            int number_changed = 0;
            for (j = 0;j < changed.size();j++)
            {
                if (changed.get(j))
                {
                    number_changed++;
                }
                 
            }
            fluctuation = ((float)number_changed / sample.size()) * 100;
            pass++;
            
            
            if (fluctuation < fluctuation_best)
            {
                
                prot_best = prot;
                prot_seq_best = prot_seq;
                fluctuation_best = fluctuation;
            }
             
        }
        
        
        results.proto = prot_best;
        results.proto_seq = prot_seq_best;
        results.fluctuation = fluctuation_best;
    }

    private final Random random = new Random();
}


