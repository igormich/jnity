define(["dojo/_base/declare", "dojo/_base/lang", "dojo/Deferred", "dojo/promise/all", "dojo/io-query", "dojo/hash", "dojo/json", "dojo/errors/CancelError",
        "dojox/lang/functional", "dojo/_base/array",
        "com/rocketsoft/kbm/core/utils/collectionUtils",
        "com/rocketsoft/kbm/core/utils/promiseUtils",
        "com/rocketsoft/kbm/core/utils/propertyMaker",
        "com/rocketsoft/kbm/cubes/dyntuples/model/TableDefinition",
        "com/rocketsoft/kbm/cubes/dyntuples/model/Navigation",
        "com/rocketsoft/kbm/cqm/common/cae/auth/serviceRequest",
        "com/rocketsoft/kbm/cqm/common/cae/beans/BeanReference",
        "com/rocketsoft/kbm/cqm/common/cae/messages/globalMessageQueue",
        "com/rocketsoft/kbm/cqm/common/cae/model/CqmSubsystem",
        "com/rocketsoft/kbm/cqm/common/cae/model/Dsgroup",
        "com/rocketsoft/kbm/cqm/common/cae/utils/ajaxUtils",
        "com/rocketsoft/kbm/cqm/dbrowse/core/cubes/QmCubeQuery",
        "com/rocketsoft/kbm/cqm/dbrowse/core/cubes/QmTableQuery",
        "com/rocketsoft/kbm/cqm/dbrowse/core/model/CascadingAppModel",
        "com/rocketsoft/kbm/cqm/dbrowse/core/model/ComparisonTableRequest",
        "com/rocketsoft/kbm/cqm/dbrowse/core/model/sortUtils",
        "com/rocketsoft/kbm/cqm/dbrowse/core/model/TableRequest",
        "com/rocketsoft/kbm/cqm/dbrowse/core/model/ThreadReferenceBean",
        "com/rocketsoft/kbm/cqm/dbrowse/cae/configurations/customviews/CustomViewBeanLibrary",
        "com/rocketsoft/kbm/cqm/dbrowse/cae/cubes/AsyncStore",
        "com/rocketsoft/kbm/cqm/dbrowse/cae/model/DBrowseSessionModel",
        "com/rocketsoft/kbm/cqm/dbrowse/cae/model/navigationUtils",
        "dojo/i18n!./../nls/strings"],
  function(declare, lang, Deferred, all, ioQuery, hash, JSON, CancelError,
           df, array,
           utils,
           promiseUtils,
           propertyMaker,
           TableDefinition,
           Navigation,
           serviceRequest,
           BeanReference,
           messageQueue,
           CqmSubsystem,
           Dsgroup,
           ajaxUtils,
           QmCubeQuery,
           QmTableQuery,
           CascadingAppModel,
           ComparisonTableRequest,
           sortUtils,
           TableRequest,
           ThreadReferenceBean,
           CustomViewBeanLibrary,
           AsyncStore,
           sessionModel,
           navigationUtils,
           i18n) {
    function packIntervals(/*array*/intervals,/*char*/ delimiter){
      function sortNumber(a,b) {
        return a - b;
      }
      intervals.sort(sortNumber);
      var intvPack = [];// contains numbers and delimiters
      intvPack[0] = intervals[0];// put first id anyway
      if (intervals.length > 1) {
        var isInterval = false;
        for (var i = 1; i < intervals.length - 1; i++) {
          if (((intervals[i] - 1) == intervals[i - 1])
              && ((intervals[i] + 1) == intervals[i + 1])) {
            isInterval = true;// skip elements of consecutive
          } else {
            intvPack.push(isInterval ? "-" : delimiter);
            intvPack.push(intervals[i]);
            isInterval = false;
          }
        }
        intvPack.push(isInterval ? "-" : delimiter);
        intvPack.push(intervals[intervals.length - 1]);//put last id anyway
      }
      return intvPack.join("");
    }

    function unpackIntervals(/*array*/intervals,/*char*/ delimiter){
      var unpacked = [];
      df.forEach(intervals.split(delimiter), function(intv) {
        if (intv.indexOf("-") >= 0) {
          // input "1-5" produce 1,2,3,4,5
          border = intv.split("-").map(Number);
          for (var i = border[0]; i <= border[1]; i++) {
            unpacked.push(i);
          }
        } else {
          unpacked.push(intv);
        }
      });
      return unpacked.join(",");
    }

    function needsReload(/*Navigaiton*/ navigation, /*BeanWrapper[CustomView]*/ oldCustomView, /*BeanWrapper[CustomView]*/ newCustomView) {
      if (navigation.inLastDrills("SQL")
                  && !oldCustomView.bean.isLevelIncluded("SQL", "ABBREV_SQL_TEXT")
                  && newCustomView.bean.isLevelIncluded("SQL", "ABBREV_SQL_TEXT")) {
        return true;
      }
      var lastDrills = navigation.getLastDrills(),
          newSort = sortUtils.extractSortFromCustomView(newCustomView.bean, lastDrills);
      if (newSort.length > 0) {
        var oldSort = sortUtils.extractSortFromCustomView(oldCustomView.bean, lastDrills);
        return !utils.areEqual(newSort, oldSort);
      } else {
        return false;
      }
    }

    function replaceCustomView(/*Map<String, BeanWrapper[CustomView]>*/ overridenCustomViews,
                               /*String*/ dataAggregateName,
                               /*BeanWrapper[CustomView]*/ bw) {
      dataAggregateName = CustomViewBeanLibrary.getDataAggregateName(dataAggregateName);
      if (bw.bean.dataAggregateName == dataAggregateName) {
        var userCustomView = overridenCustomViews[bw.shared + "-" + bw.name];
        return userCustomView ? userCustomView : bw;
      }
    }
    
    function updateDataProviderFromBeanLibrary(/*Function*/ modelSetter, /*String*/ firstItem, state) {
      if (state.privateBeans.loading || state.sharedBeans.loading) return;
      var privateBeans = state.privateBeans.error ? [] : state.privateBeans.data.getBeanWrappers(),
          sharedBeans = state.sharedBeans.error ? [] : state.sharedBeans.data.getBeanWrappers();
      modelSetter.call(this, [firstItem].concat(privateBeans, sharedBeans));
    }
    
    function configsEqual(/*Object*/ s1, /*Object*/ s2) {
      return lang.isString(s1) || lang.isString(s2)
                    ? s1 === s2
                    : s1.bean.equals(s2.bean);
    }
    
    var nonTopNColumn = {
      AVG_X_CPU : true,
      AVG_X_DELAYS : true,
      AVG_X_ELAPSED : true,
      AVG_X_GETPAGES : true,
      BUFFER_POOL_HIT_RATIO : true,
      CPU_AVERAGE : true,
      CPU_HPERCENT : true, 
      CPU_PERCENT : true,
      DELAY_AVERAGE : true,
      DELAY_HPERCENT : true,
      DELAY_PERCENT : true,
      ELAPSED_AVERAGE : true,
      ELAPSED_PERCENT : true,
      GETPAGES_AVERAGE : true,
      GLOBAL_CONT_DLY : true,
      GLOBAL_CONT_EVT : true,
      HIPER_POOL_HIT_RATIO : true,
      SERVTASK_SW_DLY : true,
      SERVTASK_SW_EVT : true,
      SYNC_IO_DLY : true,
      SYNC_IO_EVT : true,
      TOTAL_EVT : true,
      UAT_PERCENT : true,
      ZIIP_CPU_HPERCENT : true
    };
        
    var AppModel = declare(CascadingAppModel, {
      CQM_SUBSYSTEMS: "CQM Subsystems",
      NO_FILTER: "no_filter",
      WITHOUT_TOPN: "without_topN",
      NO_BASELINE: "no_baseline",
      NO_DB2: "All",
      
      constructor: function(/*ClientPrefs*/ clientPrefs, /*Array[BeanCollection]*/ customViewsBeanCollections,
                            /*BeanLibrary*/ filterBeanLibrary, /*BeanLibrary*/ offloadBeanLibrary, /*BeanLibrary*/ customViewBeanLibrary, /*BeanLibrary*/ baselinesBeanLibrary, /*BeanLibrary*/ stagingTableBeanLibrary,
                            /*CubeRelations*/ backstoreCubeRelations, /*CubeRelations*/ offloadCubeRelations) {
        this._filterBeanLibrary = filterBeanLibrary;
        this._offloadBeanLibrary = offloadBeanLibrary;
        this._customViewBeanLibrary = customViewBeanLibrary;
        this._baselinesBeanLibrary = baselinesBeanLibrary;
        this._stagingTableBeanLibrary = stagingTableBeanLibrary;
        
        this._backstoreCubeRelations = backstoreCubeRelations;
        this._offloadCubeRelations = offloadCubeRelations;
        
        this._cubeToCustomView = clientPrefs.getPreferredViews();
        
        this._userCubeToCustomView = {}; // Map<String, BeanReference>
        this._overridenCustomViews = {}; // Map<String, BeanWrapper[CustomView]>
        this._overridenSorts = {};
        this._dirtyCustomViews = false;
  
        this._allTables = df.filter(backstoreCubeRelations.getAllDataAggregates(), function(a) {
          return a instanceof TableDefinition && a.getName() != "intervals"
                                              && a.getName() != "excpparact"
                                              && a.getName() != "curparact";
        });
        this._defaultCustomViews = this._chooseDefaultCustomViews({privateBeans: {data: customViewsBeanCollections[0]},
                                                                   sharedBeans: {data: customViewsBeanCollections[1]}},
                                                                   clientPrefs);
        this._lastCustomViewsState = {privateBeans: {data: customViewsBeanCollections[0]}, sharedBeans: {data: customViewsBeanCollections[1]}};
        
        this.registerArrayBasedNode("source", [], function() {
            var subsystemsString = this.CQM_SUBSYSTEMS;
            return all([this._offloadBeanLibrary.getBeansPromise(false), this._offloadBeanLibrary.getBeansPromise(true)]).then(function(data) {
                return [subsystemsString].concat(data[0].getBeanWrappers(), data[1].getBeanWrappers());
              });
          }, this.beanWrapperToKey, undefined, false, undefined, configsEqual);
        this.registerArrayBasedNode("target", ["source"], this.getTargetsFromSource.bind(this, this.getSelectedSource),
                                                          df.lambda("_.getFullName()"),
                                                          this.chooseBestTarget.bind(this, this.getSelectedSource));
        this.registerArrayBasedNode("interval", ["target"], function() {
            return this.getSelectedTarget().loadIntervals();
          }, df.lambda(".number"), null, true);
        
        this.registerArrayBasedNode("db2", ["target"], function() {
          return [this.NO_DB2].concat(this.getSelectedTarget().getDb2s());
        }, undefined, undefined, false, function(dataProvider, newKey) {
          return {dataProvider: dataProvider.concat(newKey), index: dataProvider.length};
        });
        
        this.registerArrayBasedNode("perspective", ["source"], function() {
            var summaries = ["summaries"],
                maybeCurAct = this.isOffload() ? [] : ["curact"];
            return summaries.concat(maybeCurAct, ["exceptions", "sqlcodes", "db2commands"]);
          }, df.lambda(".toLowerCase()"));
        
        this.registerArrayBasedNode("initialcommand", ["perspective"], function() {
            var cubeRelations = this.isOffload() ? this._offloadCubeRelations : this._backstoreCubeRelations;
            return cubeRelations.getInitialDrills(this.getSelectedPerspective());
          }, df.lambda(".getName()"), undefined, true);
        
        this.registerSingleValueNode("commands", ["initialcommand"], function(key) {
            var cubeRelations = this.isOffload() ? this._offloadCubeRelations : this._backstoreCubeRelations,
                nav;
            if (key) {
              return navigationUtils.initFromHash(cubeRelations, this._defaultCustomViews, key);
            } else {
              var navigation = new Navigation({cubeRelations: cubeRelations,
                                               cubeQueryClass: QmCubeQuery, tableQueryClass: QmTableQuery,
                                               tableCustomViews: this._defaultCustomViews});
              return navigation.setInitialDrills(this.getSelectedInitialcommand())
                               .setTableCustomViews(this._defaultCustomViews);
            }
          }, navigationUtils.makeStringHash.bind(navigationUtils));

        this._sortFromPath = undefined;
        this.registerSingleValueNode("sort", ["commands"], function(key) {
          if (this._sortFromPath === undefined) {
            this._sortFromPath = this._sortKeyToValue(key);
          }
          return this._sortFromPath;
        });
        
        this.registerSingleValueNode("activeFilterProperty", [], df.lambda("true"));//df.lambda("_ === true"));
        this.registerArrayBasedNode("filter", ["activeFilterProperty", "perspective"], function() {
            if (this.getActiveFilterProperty() && this.getSelectedPerspective() != "sqlcodes") {
              var noFilter = this.NO_FILTER;
              return all([this._filterBeanLibrary.getBeansPromise(false), this._filterBeanLibrary.getBeansPromise(true)]).then(function(data) {
                  return [noFilter].concat(data[0].getBeanWrappers(), data[1].getBeanWrappers());
                });
            } else {
              return [this.NO_FILTER];
            }
          }, this.beanWrapperToKey, undefined, false, undefined, configsEqual);
        
        this.registerSingleValueNode("activeCustomViewProperty", [], df.lambda("_ === true"));
        this.registerArrayBasedNode("customView", ["activeCustomViewProperty", "commands"], function() {
            var beanLibrary = this.getCustomViewBeanLibrary(),
                navigation = this.getCommands(),
                dataAggregateName = navigation.getDataAggregateName(),
                replaceCustomViewFunc = replaceCustomView.bind(null, this._overridenCustomViews, dataAggregateName);
            return beanLibrary.getCustomViewsForDataAggregate(dataAggregateName).then(function(bws) {
              return df.map(bws, replaceCustomViewFunc);
            });
          }, df.lambda(".makeBeanReference().asString()"), function(dataProvider) {
            var dataAggregateName = this.getCommands().getDataAggregateName(),
                dataAggregateName = CustomViewBeanLibrary.getDataAggregateName(dataAggregateName),
                currentCustomView = this._userCubeToCustomView[dataAggregateName],
                prefCustomView = this._cubeToCustomView[dataAggregateName],
                idx = -1;
            if (idx == -1 && currentCustomView) {
              idx = utils.indexOf(dataProvider, function(bw) {return bw.makeBeanReference().equals(currentCustomView);});
            }
            if (idx == -1 && prefCustomView) {
              idx = utils.indexOf(dataProvider, function(bw) {return bw.makeBeanReference().equals(prefCustomView);});
            }
            return idx != -1 ? idx : 0;
          });
          
        var operSummariesCube = backstoreCubeRelations.getDataAggregate("opersummaries"),
            topNDataProvider = utils.flatMap(operSummariesCube.getMeasures(), function(meas) {
              if (!(meas.getName() in nonTopNColumn)){
                                  return {value: meas.getName(), label: meas.getLongNameIfAny()};
                                }
                              }); 
        
        this.fullDataProvider = [{value: this.WITHOUT_TOPN, label: "No TopN"}, {}].concat(topNDataProvider);
        this.registerSingleValueNode("activeTopNProperty", [], df.lambda("true"));// df.lambda("_ === true"));
        this.registerArrayBasedNode("topN", ["activeTopNProperty"], function() {
          return this.fullDataProvider;
        }, df.lambda(".value"));
        
        this.registerSingleValueNode("activeCompareProperty", [], df.lambda("_ === true"));
        
        this.on("commandsUpdated", function(evt) {
          var navigation = evt.value,
              aggregateName = navigation.getDataAggregateName(),
              compareAvailable = aggregateName == "opersummaries" || aggregateName == "structsummaries";
          if (!compareAvailable && this.isActiveComparePropertyLoaded() && this.getActiveCompareProperty()) {
            this.setActiveComparePropertyKey(false, true);
          }
        }.bind(this));
        this.cloneAndRegisterNode("source", "baselineSource", ["activeCompareProperty"]);
        this.registerArrayBasedNode("baselineTarget", ["baselineSource"],
                                    this.getTargetsFromSource.bind(this, this.getSelectedBaselineSource),
                                    df.lambda("_.getFullName()"),
                                    this.chooseBestTarget.bind(this, this.getSelectedBaselineSource));
        this.registerArrayBasedNode("baselineInterval", ["baselineTarget"], function() {
          return this.getSelectedBaselineTarget().loadIntervals();
        }, df.lambda(".number"), null, true);
        this.registerArrayBasedNode("baselineDb2", ["baselineTarget"], function() {
          return [this.NO_DB2].concat(this.getSelectedBaselineTarget().getDb2s());
        }, undefined, undefined, false, function(dataProvider, newKey) {
          return {dataProvider: dataProvider.concat(newKey), index: dataProvider.length};
        });
        this.on("activeComparePropertyUpdated", function(evt) {
          var nodes = {baselineSource: true, baselineTarget: true, baselineInterval: true, baseline: true, db2: true, baselineDb2: true};
          evt.value ? this.batchEnable(nodes) : this.batchDisable(nodes);
        });
        this.displayModes = [
          {compareMode: "ABSOLUTE", displayMode: "DEFAULT"}, 
          {compareMode: "ABSOLUTE", displayMode: "RETAINED"},
          {compareMode: "ABSOLUTE", displayMode: "ADDED"},
          {compareMode: "ABSOLUTE", displayMode: "REMOVED"},
          {compareMode: "PERCENT", displayMode: "RETAINED"},
          {compareMode: "ABSOLUTE", displayMode: "BASELINE"},
          {compareMode: "ABSOLUTE", displayMode: "CURRENT"}
        ];
        this.registerArrayBasedNode("compareMode", ["activeCompareProperty"], function() {
          return ["ABSOLUTE", "PERCENT"];
        });
        this.registerArrayBasedNode("displayMode", ["compareMode"], function() {
          var selectedCompareMode = this.getSelectedCompareMode();
          return this.displayModes.filter(function (x) { return x.compareMode === selectedCompareMode; }).map(function (x) {
            return x.displayMode;
          });
        });
        this.registerSingleValueNode("compareModeDisplayMode", ["compareMode", "displayMode"], function () {
          return {compareMode: this.getSelectedCompareMode(), displayMode: this.getSelectedDisplayMode()};
        });
        this.registerArrayBasedNode("baseline", ["activeCompareProperty"], function() {
          if (this.getActiveCompareProperty()) {
            var adHoc = this.NO_BASELINE;
            return all([this._baselinesBeanLibrary.getBeansPromise(false), this._baselinesBeanLibrary.getBeansPromise(true)]).then(function(data) {
              return [adHoc].concat(data[0].getBeanWrappers(), data[1].getBeanWrappers());
            });
          } else {
            return [""];
          }
        }, this.beanWrapperToKey, undefined, false, undefined, configsEqual);
        
        this.registerDefaultNode("go", ["interval", "commands", "filter", "customView", "topN", "baselineInterval", "baseline"]);
        
        var that = this;
        this.on("targetUpdated", function(evt) {
          if (that._intervalsListener) that._intervalsListener.remove();
          that._intervalsListener = evt.selectedValue.on("intervalsLoaded", function(intvs) {
            if (that.isIntervalLoaded()) that.setIntervalDataProvider(intvs);
          });
        });
        this.on("baselineTargetUpdated", function(evt) {
          if (that._baselineIntervalsListener) that._baselineIntervalsListener.remove();
          that._baselineIntervalsListener = evt.selectedValue.on("intervalsLoaded", function(intvs) {
            if (!that.isBaselineIntervalDisabled() && that.isBaselineIntervalLoaded()) that.setBaselineIntervalDataProvider(intvs);
          });
        });
        
        filterBeanLibrary.getStateProperty().onValue(updateDataProviderFromBeanLibrary.bind(this, this.setFilterDataProvider, this.NO_FILTER));
        baselinesBeanLibrary.getStateProperty().onValue(updateDataProviderFromBeanLibrary.bind(this, this.setBaselineDataProvider, this.NO_BASELINE));
        
        propertyMaker.combineAsArray(customViewBeanLibrary.getStateProperty(),
                                     sessionModel.getClientPrefsProperty(),
                                     // small hack in order to prevent updating table when custom views are updating
                                     customViewBeanLibrary.getUpdatingProperty().delay(0))
                     .onValues(function(customViewsState, clientPrefsState, updating) {
          var clientPrefsLoaded = !clientPrefsState.loading && clientPrefsState.data;
          if (clientPrefsLoaded) {
            this._cubeToCustomView = clientPrefsState.data.getPreferredViews();
          }
          
          if (updating || customViewsState.privateBeans.loading || customViewsState.sharedBeans.loading || !this.isCommandsLoaded()) return;
          if (this.isCustomViewLoaded()) {
            var oldSelectedCustomViewWrapper = this.getSelectedCustomView();
            this._lastCustomViewsState = customViewsState;
            this._updateCustomViewsDataProvider();
            
            var navigation = this.getCommands();
                newSelectedCustomViewWrapper = this.getSelectedCustomView();
            if (needsReload(navigation, oldSelectedCustomViewWrapper, newSelectedCustomViewWrapper)) {
              // reload page because we need ABBREV_TEXT
              this.executeRequest();
            } else {
              // if selected custom view is the same then fire an update manually because
              // appModel doesn't compare internal content of beans.
              if (oldSelectedCustomViewWrapper.shared == newSelectedCustomViewWrapper.shared
                      && oldSelectedCustomViewWrapper.name == newSelectedCustomViewWrapper.name) {
                this.emit("currentCustomViewChanged", newSelectedCustomViewWrapper);
              }
            }
          }
          
          if (clientPrefsLoaded) {
            this._defaultCustomViews = this._chooseDefaultCustomViews(customViewsState, clientPrefsState.data);
            if (this.isCommandsLoaded()) {
              var updatedNavigation = this.getCommands().setTableCustomViews(this._defaultCustomViews);
              this.setCommands(updatedNavigation, true);
            }
          }
        }.bind(this));
      },

      /*
       * OffloadBeanLibrary populates offloads into model and model starts live (loading data, firing events and so on).
       * We should not do it unless application is ready (all listeners are attached to model and model is initialized
       * by data from URL). So this method helps to delay the start.
       */
      start: function() {
        this.getOffloadBeanLibrary().getStateProperty().onValue(function(state) {
          updateDataProviderFromBeanLibrary(this.setSourceDataProvider, this.CQM_SUBSYSTEMS, state);
          updateDataProviderFromBeanLibrary(this.setBaselineSourceDataProvider, this.CQM_SUBSYSTEMS, state);
        }.bind(this));
      },
      
      chooseBestTarget: function(/*Function*/ sourceGetter, /*Array[Target]*/ targets) {
        var source = sourceGetter.apply(this);
        if (source === this.CQM_SUBSYSTEMS) {
          var sysname = sessionModel.getPrimarySystem();
          var result = utils.indexOf(targets, function(target) {
            return target.isCompatible() && (sysname === null || (target.isInstanceOf(CqmSubsystem) && target.getSysname() === sysname)
                                                              || (target.isInstanceOf(Dsgroup) && array.indexOf(target.getSystems(), sysname) !== -1));
          });
          return result != -1 ? result : 0;
        } else {
          var smfId = sessionModel.getPrimarySmfId();
          if (smfId) {
            var result = utils.indexOf(targets, function(target) {
              return (target.isInstanceOf(CqmSubsystem) && target.getSmfId() === smfId)
                       || (target.isInstanceOf(Dsgroup) && array.indexOf(target.getSmfids(), smfId) !== -1);
            });
            return result != -1 ? result : 0;
          } else {
            return 0;
          }
        }
      },
      // method also is called inside in BaselineEditor
      getTargetsFromSource: function(/*Function*/ sourceGetter) {
        var source = sourceGetter.apply(this);
        if (source != this.CQM_SUBSYSTEMS) {
          collectorsModel = sessionModel.getOffloadCollectorsModel(source.shared, source.name, source.bean);
        } else {
          collectorsModel = sessionModel.getCollectorsModel();
        }
        return all([collectorsModel.getSubsystems(), collectorsModel.getDsgroups()]).then(function(data) {
          return data[0].concat(data[1]);
        });
      },
      isOffload: function() {
        return this.getSelectedSource() != this.CQM_SUBSYSTEMS;
      },
      
      _currentTableRequest: null,
      executeRequest: function() {
        if (!this.isGoLoaded()) {
          return;
        }
        
        var target = this.getSelectedTarget(),
            intervals = this.getSelectedInterval(),
            navigation = this.getCommands(),
            dataAggregate = this._backstoreCubeRelations.getDataAggregate(navigation.getDataAggregateName());
        var selectedFilter = this.getSelectedFilter(),
            filterBean = selectedFilter != this.NO_FILTER ? selectedFilter.bean : null;
        var rawTopNColumn = this.getSelectedTopN().value,
            topNApplicable = utils.findWhere(navigation.getLastDrills(), ".getName()", "SQL"),
            topNColumn = rawTopNColumn != this.WITHOUT_TOPN && topNApplicable ? rawTopNColumn : null;
        
        var TableRequestClass = TableRequest,
            baselineTarget, baselineIntervals, compareMode, displayMode;
        if (this.getActiveCompareProperty()) {
          compareMode = this.getSelectedCompareMode();
          displayMode = this.getSelectedDisplayMode();
          switch (compareMode) {
          case "ABSOLUTE":
            switch (displayMode) {
            case "CURRENT":
              break;
            case "BASELINE":
              target =  this.getSelectedBaselineTarget();
              intervals = this.getSelectedBaselineInterval();
              break;
            default:
              TableRequestClass = ComparisonTableRequest;
              baselineTarget =  this.getSelectedBaselineTarget();
              baselineIntervals = this.getSelectedBaselineInterval();
            }
            break;
          case "PERCENT":
            TableRequestClass = ComparisonTableRequest;
            displayMode = "RETAINED";
            baselineTarget =  this.getSelectedBaselineTarget();
            baselineIntervals = this.getSelectedBaselineInterval();
            break;
          }
        } else {
          TableRequestClass = TableRequest;
        }
        
        var sourceDb2, baselineDb2;
        if (TableRequestClass == ComparisonTableRequest) {
          var rawSourceDb2 = this.getSelectedDb2(),
              rawBaselineDb2 = this.getSelectedBaselineDb2(),
              sourceDb2 = rawSourceDb2 != this.NO_DB2 ? rawSourceDb2 : null,
              baselineDb2 = rawBaselineDb2 != this.NO_DB2 ? rawBaselineDb2 : null;
          
          if ((sourceDb2 && !baselineDb2) || (!sourceDb2 && baselineDb2)) {
            this.emit("wrongTableRequest", {message: "If you specify either a baseline DB2 or a current DB2, you must specify both."});
            return;
          }
        }

        var customViewWrapper = this.getSelectedCustomView();
        if (this._sortFromPath) {
          var sortData = this.getSort();
          sortedCustomViewWrapper = customViewWrapper.bean.sort(navigation.getLastDrills(), sortData);
          customViewWrapper = lang.mixin(null, customViewWrapper, {bean: sortedCustomViewWrapper, sortData: sortData, dirty: true});
          this.updateCustomView(customViewWrapper);
          this._sortFromPath = null;
        }

        var tr = this._currentTableRequest = new TableRequestClass({target: target,
                                                                    baselineTarget: baselineTarget,
                                                                    filter: filterBean,
                                                                    navigation: navigation,
                                                                    dataAggregate: dataAggregate,
                                                                    intervals: intervals,
                                                                    baselineIntervals: df.map(baselineIntervals || [], ".number"),
                                                                    baselineIntervalObjs: baselineIntervals,
                                                                    compareMode: compareMode,
                                                                    displayMode: displayMode,
                                                                    //sort: this.getSort(),
                                                                    customViewWrapper: customViewWrapper,
                                                                    topNColumn: topNColumn,
                                                                    sourceDb2: sourceDb2,
                                                                    baselineDb2: baselineDb2,
                                                                    asyncStoreClass: AsyncStore});
        
        var newHash = this.objectToQuery(this.makeHash());
        window.cqm_app_hash_update = this.isHashChanged(newHash);
        hash(newHash);
        this.emit("tableRequestUpdated", tr);
        return tr;
      },
      
      cancelThread: function(/*Map<String, Object>*/ row) {
        if (!this.isGoLoaded()) {
          return;
        }
        var target = this.getSelectedTarget(),
            threadReferenceBean = new ThreadReferenceBean(target, row),
            args = ajaxUtils.makeDefaultOptions(JSON.stringify(threadReferenceBean.toJson(true))),
            messageId = messageQueue.start("Cancelling thread on " + target.getFullName());
        return serviceRequest.doPost("/webclient/qmQuery/cancelThread", args)
            .then(messageQueue.end.bind(messageQueue, messageId, "Thread is cancelled"),
                  ajaxUtils.errorHandler.bind(ajaxUtils, "Can not execute query on " + target.getFullName(), messageId));
      },
      
      decacheSqlCodes: function() {
        var messageId = messageQueue.start("Clearing SQL Codes cache");
        return serviceRequest.doDelete("/webclient/qmQuery/refresh-sql-distrib", ajaxUtils.makeDefaultOptions())
           .then(messageQueue.end.bind(messageQueue, messageId, "SQL Codes cache is removed"),
                 ajaxUtils.errorHandler.bind(ajaxUtils, "Can not clear SQL Codes cache", messageId));
      },
      
      setNavigation: function(/*Navigation*/ navigation, /*boolean*/ needViewUpdate) {
        if (this.isActiveComparePropertyLoaded()) {
          if (this.getActiveCompareProperty()
                          && navigation.getDataAggregate() instanceof TableDefinition) {
            throw new Error(i18n.appModel.noSuchDrilldownInCompareMessage);
          } else if (this.isDb2Loaded() && !this.isDb2Disabled()
                            && (navigation.hasFilter("DB2_SUBSYSTEM") || navigation.hasFilter("DB2STRUCT")
                                        || navigation.inLastDrills("DB2_SUBSYSTEM") || navigation.inLastDrills("DB2STRUCT"))
                            && this.getSelectedDb2() != this.NO_DB2 && this.getSelectedBaselineDb2() != this.NO_DB2) {
            throw new Error(i18n.appModel.onlyOneDb2InComboBoxMessage);
          } else if (navigation.getLastDrill() == this._backstoreCubeRelations.getDrillByName("exceptions", "exceptionobjects")
                            && !this.isOffload()
                            && navigation.getFilterValue("OBJECT_COUNT") === 0) {
            throw new Error(i18n.appModel.noObjectsMessage);
          } else {
            this.setCommands(navigation, needViewUpdate);
            return true;
          }
        } else {
          // not sure what I should do in that case
          console.warn("setNavigation during update");
          this.setCommands(navigation, needViewUpdate);
          return true;
        }
      },
      
      saveSort: function(/*String*/ aggregateName, /*Object[]*/ sort) {
        var o = this._overridenSorts[aggregateName] = sort.length != 0 ? sort : null;
        // We should update node only if node is loaded and chosen table is the same as parameter "aggregateName".
        // If sort node is not loaded updating _overridenSorts is enough. That value will be taken from _overridenSorts
        // when node is updated.
        if (this.isSortLoaded() && this.getCommands().getDataAggregateName() == aggregateName) {
          this.setSort(o);
        }
      },
      updateCustomView: function(/*BeanWrapper[CustomView]*/ beanWrapper) {
        // if (utils.isEmpty(this._overridenCustomViews)) {
       this._dirtyCustomViews = true;
          // this.emit("dirtyCustomView", {dirty: true});
        // }
        this._overridenCustomViews[beanWrapper.shared + "-" + beanWrapper.name] = beanWrapper;
        this._updateCustomViewsDataProvider();
      },
      setCurrentCustomView: function(/*BeanReference*/ beanReference, /*BeanWrapper[CustomView]*/ beanWrapper) {
        // if (utils.isEmpty(this._userCubeToCustomView)) {
        this._dirtyCustomViews = true;
          // this.emit("dirtyCustomView", {dirty: true});
        // }
        var dataAggregateName = beanWrapper.bean.dataAggregateName;
        this._userCubeToCustomView[dataAggregateName] = beanReference;
        if (this.isCommandsLoaded() && this.getCommands().getDataAggregateName() == dataAggregateName) {
          var oldSelectedCustomViewWrapper = this.getSelectedCustomView();
          this.setSelectedCustomViewKey(beanReference.asString());
          var overridenCustomView = this._overridenCustomViews[beanWrapper.shared + "-" + beanWrapper.name],
              newSelectedCustomViewWrapper = overridenCustomView ? overridenCustomView : beanWrapper;
          if (needsReload(this.getCommands(), oldSelectedCustomViewWrapper, newSelectedCustomViewWrapper)) {
            // reload page because we need ABBREV_TEXT
            this.executeRequest();
          } else {
            this.emit("currentCustomViewChanged", newSelectedCustomViewWrapper);
          }
        }
      },
      isCustomViewsChanged: function() {
        return this._dirtyCustomViews;
      },
      saveDirtyCustomViews: function() {
        var promises = df.map(this._overridenCustomViews, function(bw) {
          var isCube = bw.bean.metricsColumns != null,
              beanClassName = isCube ? "com.rocketsoft.nm.qm.commoncubes.customviews.CubeCustomView"
                                     : "com.rocketsoft.nm.qm.commoncubes.customviews.TableCustomView";
          return this._customViewBeanLibrary.post(bw.shared, bw.name, beanClassName, bw.bean);
        }, this);
        var promise = sessionModel.updatePreferredViews(this._userCubeToCustomView);
        return all(promises.concat(promise)).then(function(arr) {
          this.cancelDirtyCustomViews();
          return arr;
        }.bind(this));
      },
      cancelDirtyCustomViews: function() {
        this._dirtyCustomViews = false;
        this._overridenCustomViews = {};
        // this._userCubeToCustomView = {};
        // this.emit("dirtyCustomView", {dirty: false});
        this._updateCustomViewsDataProvider();
      },
      removeOverridenCustomView: function(/*boolean*/ shared, /*String*/ beanName, /*boolean*/ withoutEvent) {
        var key = shared + "-" + beanName,
            overridenCustomView = this._overridenCustomViews[key];
        if (overridenCustomView) {
          delete this._overridenCustomViews[key];
          if (utils.isEmpty(this._overridenCustomViews)) {
            this._dirtyCustomViews = false;
            // this.emit("dirtyCustomView", {dirty: false});
          }
        }
        this._updateCustomViewsDataProvider();
        if (!withoutEvent) {
          var bw = this._lastCustomViewsState[shared ? "sharedBeans" : "privateBeans"].data.getBeanWrapper(beanName);
          this.emit("currentCustomViewChanged", bw);
        }
        return overridenCustomView;
      },
      _updateCustomViewsDataProvider: function() {
        if (this.isCustomViewLoaded()) {
          var dataAggregateName = this.getCommands().getDataAggregateName(),
              dataAggregateName = CustomViewBeanLibrary.getDataAggregateName(dataAggregateName),
              replaceCustomViewFunc = replaceCustomView.bind(null, this._overridenCustomViews, dataAggregateName),
              privateBeans = utils.flatMap(this._lastCustomViewsState.privateBeans.data.getBeanWrappers(), replaceCustomViewFunc),
              sharedBeans = utils.flatMap(this._lastCustomViewsState.sharedBeans.data.getBeanWrappers(), replaceCustomViewFunc),
              dataProvider = privateBeans.concat(sharedBeans);
          this.setCustomViewDataProvider(dataProvider);
          this.emit("customViewsDataProviderChanged", {dataProvider: dataProvider, selectedValue: this.getSelectedCustomView()});
        }
      },
      
      _chooseDefaultCustomViews: function(customViewsState, clientPrefs) {
        var result = df.foldl(this._allTables, function(z, tableDef) {
          var tableName = tableDef.getName(),
              customViewReference = clientPrefs.getPreferredView(tableName),
              tableCustomView;
          if (customViewReference) {
            var beanColl = customViewsState[customViewReference.isShared() ? "sharedBeans" : "privateBeans"].data;
            tableCustomView = beanColl.getBean(customViewReference.getName());
          }
          if (!tableCustomView) {
            var idx = utils.indexOf(customViewsState.privateBeans.data.getBeanWrappers(), function(bw) {
              return bw.bean.dataAggregateName == tableName;
            });
            tableCustomView = customViewsState.privateBeans.data.getBeanWrappers()[idx].bean;
          }
          z[tableName] = tableCustomView;
          return z;
        }, {});
        result.curparact = result.curact;
        result.excpparact = result.exceptions;
        return result;
      },
      
      beanWrapperToKey: function(/*BeanWrapper[Object]*/ bw) {
        return lang.isString(bw) ? "" : bw.makeBeanReference().asString();
      },
      _sortValueToKey: function(/*Array[SortInfo]*/ columns) {
        return df.map(columns, function(sort) {
          var colId = sort.getLevelName() || sort.getComponentName();
          return sort.isDescending() ? "!" + colId : colId;
        }).join(",");
      },
      _sortKeyToValue: function(key) {
        if (!key || key == "") {
          return null;
        }
        return df.map(key.split(","), function(str) {
            if (str.charAt(0) == "!") {
              return {descending: true, colId: str.slice(1)};
            } else {
              return {descending: false, colId: str};
            }
          });
      },
      
      getFilterBeanLibrary: function() {
        return this._filterBeanLibrary;
      },
      getOffloadBeanLibrary: function() {
        return this._offloadBeanLibrary;
      },
      getBaselineBeanLibrary: function() {
        return this._baselinesBeanLibrary;
      },
      getCustomViewBeanLibrary: function() {
        return this._customViewBeanLibrary;
      },
      getStagingTableBeanLibrary: function() {
        return this._stagingTableBeanLibrary;
      },

      getBackstoreCubeRelations: function() {
        return this._backstoreCubeRelations;
      },
      
      initFromHash: function(hash) {
        var filterActive = hash.filtOn == "true",
            keys = {
                source: hash.ol ? hash.ol : "",
                target: hash.target,
                interval: hash.intv ? hash.intv : null,
                perspective: navigationUtils.mapDataAggregateNameToPerspectiveName(hash.ds),
                initialcommand: hash.cmds ? hash.cmds : null,
                commands: ioQuery.objectToQuery({ds: hash.ds, cmds: hash.cmds, pth: hash.pth}),
                sort: hash.sort,
                activeFilterProperty: filterActive
              };
        if (keys.interval) {
          keys.interval = unpackIntervals(keys.interval, ",");
        }
        keys.filter = filterActive ? hash.filt : "no_filter";
        if (this.isActiveCustomViewPropertyEmpty()) {
          keys.activeCustomViewProperty = false;
        }
        if (hash.topN) {
          keys.activeTopNProperty = true;
          keys.topN = hash.topN.split(",")[1];
        } else {
          keys.activeTopNProperty = false;
        }
        if (hash.compare) {
          keys.activeCompareProperty = true;
          var arr = hash.baseline.split(";;");
          if (arr.length == 1) {
            keys.baseline = arr[0];
          } else {
            var lastIndexOf = array.lastIndexOf(arr[0], ":"),
                baselineSource = arr[0].substr(lastIndexOf + 1),
                baselineTarget = arr[0].substr(0, lastIndexOf);
            keys.baselineSource = baselineSource;
            keys.baselineTarget = baselineTarget;
            keys.baselineInterval = unpackIntervals(arr[1].substr(1, arr[1].length - 2), ",");
            keys.baselineDb2 = arr[2] ? arr[2] : null;
          }
          keys.compareMode = hash.compareDisplay == "ABS" ? "ABSOLUTE" : "PERCENT";
          keys.displayMode = hash.compareMode == "ALL" ? "DEFAULT" : hash.compareMode;
          keys.db2 = hash.sourceDb2;
        } else {
          keys.activeCompareProperty = false;
        }
        this.batchUpdate(keys);
      },
      makeHash: function() {
        var result = {};
        var source = this.getSelectedSource();
        if (source != this.CQM_SUBSYSTEMS) {
          result.ol = this.beanWrapperToKey(source);
        }
        result.target = this.getSelectedTarget().getFullName();
        result.ds = this.getSelectedPerspective().toLowerCase();
        var navigation = this.getCommands();
        result = navigationUtils.makeHash(result, navigation);
        
        var intvs = this.getSelectedInterval();
        if (intvs.length != 1 || !intvs[0].current) {
          result.intv = packIntervals(df.map(intvs, ".number"), ",");
        }
        
        var filt = this.getSelectedFilter();
        if (filt == this.NO_FILTER) {
          result.filtOn = false;
        } else {
          result.filtOn = true;
          result.filt = this.beanWrapperToKey(filt);
        }
        
        var topNColumn = this.getSelectedTopN().value;
        if (topNColumn != this.WITHOUT_TOPN) {
          result.topN = "CQM31_SUMM_METRICS," + topNColumn + ",false,true";
        }
        
        var compare = this.getActiveCompareProperty();
        if (compare) {
          var baselineWrapper = this.getSelectedBaseline();
          var baselineIntervals = packIntervals(df.map(this.getSelectedBaselineInterval(), ".number"), ",");
          if (lang.isString(baselineWrapper)) {
            result.baseline = [this.getSelectedBaselineTarget().getFullName() + ":" + this.beanWrapperToKey(this.getSelectedBaselineSource()),
                               "[" + baselineIntervals + "]",
                               this.getSelectedBaselineDb2() != this.NO_DB2 ? this.getSelectedBaselineDb2() : "",
                               "master-subsystem"].join(";;");
            var sourceDb2 = this.getSelectedDb2();
            if (sourceDb2 != this.NO_DB2) {
              result.sourceDb2 = sourceDb2;
            }
          } else {
            result.baseline = baselineWrapper.makeBeanReference().asString();
          }
          var compareMode = this.getSelectedCompareMode(),
              displayMode = this.getSelectedDisplayMode();
          result.compareDisplay = compareMode == "ABSOLUTE" ? "ABS" : "PERCENTAGE";
          result.compareMode = displayMode == "DEFAULT" ? "ALL" : displayMode;
          result.compare = true;
        }

        var customViewWrapper = this.getSelectedCustomView();
        if (customViewWrapper.sortData && customViewWrapper.sortData.length > 0) {
          var beanCollection = this._lastCustomViewsState[customViewWrapper.shared ? "sharedBeans" : "privateBeans"],
              initialCustomViewWrapper = beanCollection.data.getBean(customViewWrapper.name),
              lastDrills = navigation.getLastDrills(),
              initSort = sortUtils.extractSortFromCustomView(initialCustomViewWrapper, lastDrills),
              currentSort = sortUtils.extractSortForNavigation(navigation, result.compare, customViewWrapper.sortData);

          if (currentSort.length != 0 && !utils.areEqual(initSort, currentSort)) {
            result.sort = this._sortValueToKey(currentSort);
          }
        }

        return result;
      },
      objectToQuery: function(map) {
        var pairs = [];
        for(var name in map) {
            var value = map[name];
            // if(value != backstop[name]){
                var assign = name + "=";
                if(lang.isArray(value)){
                    for(var i = 0, l = value.length; i < l; ++i){
                        pairs.push(assign + value[i]);
                    }
                }else{
                    pairs.push(assign +value);
                }
            // }
        }
        return pairs.join("&");
      },
      isHashChanged: function(/*String*/ newHash) {
        var hash = location.hash;
        if (hash.charAt(0) == "#") {
          hash = hash.substring(1);
        }
        return hash != newHash;
      }
    });
    AppModel.packIntervals = packIntervals;
    return AppModel;
  });