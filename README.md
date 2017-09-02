# Decision Stream

<br/>

<br/>

<img src="img/desision_stream.jpg" width="60%"/>

<br/>

<br/>

#### 1. Overview [[paper]](https://arxiv.org/pdf/1704.07657v2.pdf)

This repository provides a basic implementation of the [Decision Stream](https://arxiv.org/abs/1704.07657) regression and classification algorithm. Unlike the classical decision tree approach, this method builds a directed acyclic graph with high degree of connectivity by merging statistically indistinguishable nodes at each iteration.
<br/><br/>

#### 2. Prerequisites and Dependencies

- [Clojure](https://clojure.org/)
- [Apache Commons Math](https://commons.apache.org/proper/commons-math/)
- [JBLAS](http://www.jblas.org/) (requires [ATLAS](http://math-atlas.sourceforge.net/) or [BLAS/LAPACK](http://www.netlib.org/lapack))
- [OpenCSV](http://opencsv.sourceforge.net/)

The dependencies are configured in the pom.xml file.
<br/><br/>

#### 3. First steps

- Extract the archive ```data.gz``` with training data by running ```tar -xvzf data.gz```
- *Optional:* rebuild ```decision-stream.jar``` with ```mvn package``` [Maven](https://maven.apache.org/) command.
<br/><br/>

#### 4. Train the model

```bash
java -jar decision-stream.jar base-directory train-data train-answers test-data test-answers learning_mode significance-threshold
```

The program takes 7 input parameters:

>```base-directory``` &nbsp; - &nbsp; path to the dataset <br/>
>```train-data``` &nbsp; - &nbsp; file with training data <br/>
>```train-answers``` &nbsp; - &nbsp; file with training answers <br/>
>```test-data``` &nbsp; - &nbsp; file with test data <br/>
>```test-answers``` &nbsp; - &nbsp; file with test answers <br/>
>```learning_mode:``` &nbsp; **```classification```** or **```regression```** &nbsp; - &nbsp; classification or regression problem <br/>
>```significance-threshold``` &nbsp; - &nbsp; threshold for merging/splitting operations <br/>

Example:

```
java -jar decision-stream.jar data/ailerons/ train_data.csv train_answ.csv test_data.csv test_answ.csv regression 0.02
```
<br/>

#### 5. Provided datasets

The datasets prepared for training in the ```data``` folder:

- [Ailerons (F16 aircraft control problem)](http://www.dcc.fc.up.pt/~ltorgo/Regression/DataSets.html)
- [Credit scoring](https://www.kaggle.com/c/GiveMeSomeCredit/data/) 
- [MNIST](http://yann.lecun.com/exdb/mnist/)
